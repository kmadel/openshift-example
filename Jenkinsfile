#!groovy
import groovy.json.JsonSlurperClassic

/**
* The following parameters are used in this pipeline (thus available as groovy variables via Jenkins job parameters):
*/
properties([
    [$class: 'ParametersDefinitionProperty', 
       parameterDefinitions: [
           [name: 'OS_URL', $class: 'StringParameterDefinition', defaultValue: 'https://api.cloudbees.openshift.com/', description: 'URL for your OpenShift v3 API instance'], 
           [name: 'OS_CREDS_DEV', $class: 'CredentialsParameterDefinition', credentialType: 'com.cloudbees.plugins.credentials.common.StandardCredentials', defaultValue: 'mobile-development-default-openshift-token', description: 'credentials for your development project, either user name / password or OAuth token'], 
           [name: 'OS_CREDS_TEST', $class: 'CredentialsParameterDefinition', credentialType: 'com.cloudbees.plugins.credentials.common.StandardCredentials', defaultValue: 'mobile-test-deployer-openshift-token', description: 'credentials for your test project'], 
           [name: 'OS_CREDS_PROD', $class: 'CredentialsParameterDefinition', credentialType: 'com.cloudbees.plugins.credentials.common.StandardCredentials', defaultValue: 'mobile-production-deployer-openshift-token', description: 'credentials for your production project'],
           [name: 'SAUCE_CREDS', $class: 'CredentialsParameterDefinition', credentialType: 'com.cloudbees.plugins.credentials.common.StandardCredentials', defaultValue: 'sauce-api-creds', description: 'credentials for your saucelabs'], 
           [name: 'OS_BUILD_LOG', $class: 'ChoiceParameterDefinition', choices: 'follow\nwait', description: 'how to handle output of start-build command, either wait or follow']
        ]
   ], [$class: 'BuildDiscarderProperty',
        strategy: [$class: 'LogRotator', numToKeepStr: '10', artifactNumToKeepStr: '10']
    ]
])


stage('build') {
    node{
        checkout scm
        sh 'mvn -DskipTests clean package'
        stash name: 'source', excludes: 'target/'
        archive includes: 'target/*.war'
    }
}
        
stage('test[unit&quality]') {
    parallel 'unit-test': {
        node {
            unstash 'source'
            sh 'mvn -Dmaven.test.failure.ignore=true test'
            step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
            if(currentBuild.result == 'UNSTABLE'){
                error "Unit test failures"
            }
        }
    }, 'quality-test': {
        node {
            unstash 'source'
            sh 'mvn sonar:sonar'
        } 
    }
}

//aborts previous runs that haven't reached this point
milestone 1
stage('deploy[development]') {
    //only allow one deployment to development at a time
    lock(resource: 'development-server', inversePrecedence: true) {
        node{
            unstash 'source'
            wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS_DEV, installation: 'oc-latest', insecure: true]) {
                oc('project mobile-development -q')

                def bc = oc('get bc -o json')
                if(!bc.items) {
            	    //TODO still a branch problem here
                    oc("new-app --name=mobile-deposit-ui --code='.' --image-stream=jboss-webserver30-tomcat8-openshift")
                    wait('app=mobile-deposit-ui', 7, 'MINUTES')
                    oc('expose service mobile-deposit-ui')
                } else {
                    oc("start-build mobile-deposit-ui --from-dir=. --$OS_BUILD_LOG")
                }
            }
        }
    }
    checkpoint 'deploy[development]-complete'
}

milestone 2
stage('deploy[test]') {
    //only allow one deployment to test at a time
    lock(resource: 'test-server', inversePrecedence: true) {
        mail to: 'apemberton@cloudbees.com', subject: "Deploy mobile-deposit-ui version #${env.BUILD_NUMBER} to test?",
            body: "Deploy mobile-deposit-ui#${env.BUILD_NUMBER} to test and start functional tests? Approve or Reject on ${env.BUILD_URL}."
        input "Deploy mobile-deposit-ui#${env.BUILD_NUMBER} to test?"
    
        node{
            wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS_TEST, installation: 'oc-latest', insecure: true]) {
                def project = oc('project mobile-development -q')
                def is = oc('get is -o json')
                def image = is?.items[0].metadata.name
            
                oc("tag $image:latest $image:test")

                project = oc('project mobile-test -q')
                def dc = oc('get dc -o json')
                if(!dc.items){
                    oc("new-app mobile-development/$image:test")
                    wait('app=mobile-deposit-ui', 7, 'MINUTES')
                    oc('expose service mobile-deposit-ui')
                }
            
                sleep time: 120, unit: 'SECONDS' //give JBoss another minute to start; probably better ways to validate
            }
        }
    }
}
    
stage('test[functional]') {
    node {
        unstash 'source'
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: SAUCE_CREDS, 
            usernameVariable: 'SAUCE_USER_NAME', passwordVariable: 'SAUCE_API_KEY']]) {
            sh "mvn verify" //TODO pass URL to test route
        }
        step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml', testDataPublishers: [[$class: 'SauceOnDemandReportPublisher', jobVisibility: 'public']]])
        
    }
    checkpoint 'test[functional]-complete'
}

milestone 3
stage('deploy[production]') {
    //only allow one deployment to production at a time
    lock(resource: 'production-server', inversePrecedence: true) {
        mail to: 'apemberton@cloudbees.com', subject: "Deploy mobile-deposit-ui version #${env.BUILD_NUMBER} to production?",
            body: "Deploy mobile-deposit-ui#${env.BUILD_NUMBER} to production? Approve or Reject on ${env.BUILD_URL}."
        input "Deploy mobile-deposit-ui#${env.BUILD_NUMBER} to production?"
    
        node{
            wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS_PROD, installation: 'oc-latest', insecure: true]) {
                def project = oc('project mobile-development -q')
                def is = oc('get is -o json')
                def image = is?.items[0]?.metadata.name
            
                oc("tag $image:test $image:production")

                project = oc('project mobile-production -q')
                def dc = oc('get dc -o json')
                if(!dc.items){
                    oc("new-app mobile-development/$image:production")
                    wait('app=mobile-deposit-ui', 7, 'MINUTES')
                    oc('expose service mobile-deposit-ui')
                }
            }
        }
    }
}

/**
* Execute OpenShift v3 'oc' CLI commands, sending output to Jenkins log console and returned 
* to the user as either JSON or a raw string.
* 
* @see: https://docs.openshift.com/enterprise/3.0/cli_reference/index.html 
*/
def oc(cmd){
    def output
    sh "set -o pipefail"
    output = sh returnStdout: true, script: "oc $cmd"
    if(output.startsWith('{')){
        output = new JsonSlurperClassic().parseText(output)
    }
    return output
}

/**
* Check for a pod matching $selector to be in 'Running' status until the given timeout. 
*/
def wait(selector, time, unit){
    timeout(time: time, unit: unit){
        waitUntil{
            sleep 5L //poll only every 5 seconds
            def pod = oc("get pods --selector='$selector' -o json")
            return pod.items[0]?.status?.phase == 'Running'
        }
    }
}
