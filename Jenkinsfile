#!groovy
import groovy.json.JsonSlurper

/**
* parameters:
* - OS_CREDS
* - OS_URL
*/

stage 'development'

    node{
        checkout scm
        sh 'mvn clean package clean'
        wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS, insecure: true]) {
            def project = oc('project mobile-development -q')
            def bc = oc('get bc -o json')
            
            //TODO perhaps find just the mobile-deposit-ui build config
            if(!bc.items) {
            	//TODO decide if new-app supports blocking or returns immediately (then insert polling hack)
                oc('new-app --name=mobile-deposit-ui jboss-webserver30-tomcat8-openshift~https://github.com/apemberton/mobile-deposit-ui.git#openshift')
            } else {
                //TODO consider verbose parameter for wait vs follow
                oc('start-build mobile-deposit-ui --from-dir=. --follow')
            }
            //oc scale
            //oc expose 
         }
    }
    
    checkpoint 'development-complete'
    input 'do you want to deploy this build to test?'

stage 'test'
     node{
        wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS, insecure: true]) {
            def project = oc('project mobile-development -q')
            def is = oc('get is -o json')
            def isName = is.items[0].metadata.name
            
            oc("tag $isName:latest $isName:test")

            project = oc('project mobile-test -q')
            def dc = oc('get dc -o json')
            if(!dc.items){
                oc("new-app mobile-development/$isName:test")
            }
            //oc scale
        }
    }
    
    checkpoint 'test-complete'
    input 'do you want to deploy this build to production?'
    
stage 'production'

    node{
        // sh 'oc tag :production'
        // oc get dc
        // if !dc
            // oc new-app  $DEVEL_PROJ_NAME/${IS_NAME}:test
        //oc scale    
    }
    
    
def oc(cmd){
    sh "oc $cmd | tee output"
    def output = readFile 'output'
    if(cmd.endsWith('-o json')){
       output = new JsonSlurper().parseText(output)
    }
    return output
}