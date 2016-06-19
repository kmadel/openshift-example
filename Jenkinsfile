#!groovy

import groovy.json.JsonSlurper

/**
* parameters:
* - OS_CREDS
* - OS_URL
*/

stage 'development'

	node{
		git 'https://github.com/apemberton/mobile-deposit-ui.git'
		sh 'mvn clean package clean'
		wrap([$class: 'OpenShiftBuildWrapper', url: OS_URL, credentialsId: OS_CREDS, insecure: true]) {
            def project = oc('project mobile-development -q')
            def bc = oc('get bc -o json')
            
            //TODO find just the mobile-deposit-ui build config
            if(!bc.items) {
                oc('new-app --name=mobile-deposit-ui jboss-webserver30-tomcat8-openshift~https://github.com/apemberton/mobile-deposit-ui.git#openshift')
            } else {
             	//TODO consider verbose parameter for wait vs follow
                oc('start-build mobile-deposit-ui --from-dir=. --follow')
            }
            
    	}
	}

	input 'do you want to deploy this build to test?'
		
stage 'test'
	node{
		// sh 'oc tag :test'
		// oc get dc
		// if !dc
			// oc new-app  $DEVEL_PROJ_NAME/${IS_NAME}:test
		//oc scale
		
		//TODO maybe run some selenium/saucelabs tests against test?		
	}
	
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