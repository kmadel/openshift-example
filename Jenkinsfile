#!groovy
import groovy.json.JsonSlurper

/**
* The following parameters are used in this pipeline (thus available as groovy variables via Jenkins job parameters):
* 
* - OS_URL - URL for your OpenShift v3 API instance
* - OS_CREDS_DEV - credentials for your development project, either user name / password or oauth token
* - OS_CREDS_TEST - credentials for your test project
* - OS_CREDS_PROD - credentials for your production project
* - OS_BUILD_LOG - how to handle output of start-build command, either 'wait' or 'follow'
*/

stage 'build'
    node{
        checkout scm
        sh 'mvn -DskipTests clean package'
        stash name: 'source', includes: '**', excludes: 'target/*'
        archive includes: 'target/*.war'
    }
        
stage 'test[unit-&-quality]'
    parallel 'unit-test': {
        node {
            unstash 'source'
            sh 'mvn test'
            step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
        }
    }, 'quality-test': {
        node {
            unstash 'source'
            sh 'mvn sonar:sonar'
        } 
    }

stage 'deploy[development]'
    node{
        unstash 'source'
        wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS_DEV, insecure: true]) {
            oc('project mobile-development -q')

            def bc = oc('get bc -o json')
            if(!bc.items) {
                oc("new-app --name=mobile-deposit-ui --code='.' --image-stream=jboss-webserver30-tomcat8-openshift")
                wait('app=mobile-deposit-ui', 5, 'MINUTES')
                oc('expose service mobile-deposit-ui')
            } else {
                oc("start-build mobile-deposit-ui --from-dir=. --$OS_BUILD_LOG")
            }
        }
    }
    checkpoint 'deploy[development]-complete'

stage 'deploy[test]'
    input 'do you want to deploy this build to test?'
    node{
        wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS_TEST, insecure: true]) {
            def project = oc('project mobile-development -q')
            def is = oc('get is -o json')
            def image = is?.items[0].metadata.name
            
            oc("tag $image:latest $image:test")

            project = oc('project mobile-test -q')
            def dc = oc('get dc -o json')
            if(!dc.items){
                oc("new-app mobile-development/$image:test")
                wait('app=mobile-deposit-ui', 5, 'MINUTES')
                oc('expose service mobile-deposit-ui')
            }
            //TODO may need to monitor and wait for the build here
        }
    }
    
stage 'test[functional]'
    node {
        unstash 'source'
        sh 'mvn verify' //TODO pass URL to test server
        step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml'])
    }
    checkpoint 'test[functional]-complete' 
    
stage 'deploy[production]'
    input 'do you want to deploy this build to production?'
    node{
        wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS_PROD, insecure: true]) {
            def project = oc('project mobile-test -q')
            def is = oc('get is -o json')
            def image = is?.items[0].metadata.name
            
            oc("tag $image:test $production")

            project = oc('project mobile-production -q')
            def dc = oc('get dc -o json')
            if(!dc.items){
                oc("new-app mobile-development/$image:test")
                wait('app=mobile-deposit-ui', 5, 'MINUTES')
                oc('expose service mobile-deposit-ui')
            }
            //TODO may need to monitor and wait for the build here
        }
    }


/**
* Execute OpenShift v3 'oc' CLI commands, sending output to Jenkins log console and returned 
* to the user as either JSON or a raw string.
* 
* @see: https://docs.openshift.com/enterprise/3.0/cli_reference/index.html 
*/
def oc(cmd){
    sh "set -o pipefail"
    sh "oc $cmd 2>&1 | tee output.jenkins"
    def output = readFile 'output.jenkins'
    if(output.startsWith('{')){
        output = new JsonSlurper().parseText(output)
    }
    sh "rm output.jenkins"
    return output
}

/**
* Check for a pod matching $selector to be in 'Running' status until the given timeout. 
*/
def wait(selector, time, unit){
    timeout(time: time, unit: unit){
        waitUntil{
            def pod = oc("get pods --selector='$selector' -o json")
            return pod.items[0]?.status?.phase == 'Running'
        }
    }
}
