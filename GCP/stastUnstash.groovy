pipeline {
  agent none

  environment {
    GOOGLE_APPLICATION_CREDENTIALS = credentials('gcp-service-account-key')
    // Load environment variables from file
    envVars = credentials('env-file-credentials')
    TERRAFORM_HOST = envVars.TERRAFORM_HOST
    TERRAFORM_USERNAME = envVars.TERRAFORM_USERNAME
    TERRAFORM_PASSWORD = envVars.TERRAFORM_PASSWORD
    COMPUTE_INSTANCE_NAME = 'my-instance'
    COMPUTE_ZONE = 'my-zone'
    COMPUTE_PROJECT = 'my-project'
  }

  stages {
    stage('Checkout') {
      agent {
        // Assign an agent for the Checkout stage
        label 'agent1'
      }
      steps {
        // Checkout source code from version control system
        git 'https://github.com/example/repo.git'
      }
    }

    stage('Build') {
      agent {
        // Assign an agent for the Build stage
        label 'agent2'
      }
      steps {
        // Build the Express application
        sh 'npm ci'
        sh 'npm run build'

        // Stash the built artifacts
        stash includes: 'dist/**', name: 'build-artifacts'
      }
    }

    stage('Test') {
      agent {
        // Assign an agent for the Test stage
        label 'agent3'
      }
      steps {
        // Unstash the built artifacts
        unstash 'build-artifacts'

        // Run unit tests on the built artifacts
        sh 'npm run test'
      }
    }

    stage('Provision Infrastructure') {
      agent {
        // Assign an agent for the Provision Infrastructure stage
        label 'agent4'
      }
      steps {
        // Use Terraform to provision infrastructure
        sh 'cd /path/to/terraform'
        sh 'terraform init'
        sh 'terraform plan -out=tfplan'
        sh 'terraform apply -auto-approve tfplan'
      }
    }

    stage('Deploy to Google App Engine') {
      agent {
        // Assign an agent for the Deploy to Google App Engine stage
        label 'agent5'
      }
      when {
        branch 'master'
      }
      steps {
        // Unstash the built artifacts
        unstash 'build-artifacts'

        // Deploy the application to Google App Engine
        sh 'gcloud app deploy --project=my-project --version=my-version app.yaml'
      }
    }

    stage('Deploy to Google Kubernetes Engine (GKE)') {
      agent {
        // Assign an agent for the Deploy to GKE stage
        label 'agent6'
      }
      when {
        branch 'master'
      }
      steps {
        // Authenticate with GKE cluster
        sh 'gcloud container clusters get-credentials my-cluster --zone=my-zone --project=my-project'

        // Unstash the built artifacts
        unstash 'build-artifacts'

        // Deploy the application to GKE
        sh 'kubectl apply -f kubernetes-deployment.yaml'
      }
    }

    stage('Deploy to Cloud Run') {
      agent {
        // Assign an agent for the Deploy to Cloud Run stage
        label 'agent7'
      }
      when {
        branch 'master'
      }
      steps {
        // Unstash the built artifacts
        unstash 'build-artifacts'

        // Deploy the application to Cloud Run
        sh 'gcloud run deploy my-service --image=gcr.io/my-project/my-image --platform=managed --region=my-region'
      }
    }

    stage('Deploy to Cloud Functions') {
      agent {
        // Assign an agent for the Deploy to Cloud Functions stage
        label 'agent8'
      }
      when {
        branch 'master'
      }
      steps {
        // Unstash the built artifacts
        unstash 'build-artifacts'

        // Deploy the application to Cloud Functions
        sh 'gcloud functions deploy my-function --runtime=nodejs14 --trigger-http'
      }
    }

    stage('Deploy to Firebase Hosting') {
      agent {
        // Assign an agent for the Deploy to Firebase Hosting stage
        label 'agent9'
      }
      when {
        branch 'master'
      }
      steps {
        // Unstash the built artifacts
        unstash 'build-artifacts'

        // Deploy the application to Firebase Hosting
        sh 'firebase deploy --project=my-project'
      }
    }
    stage('Notify') {
      agent {
        // Assign an agent for the Notify stage
        label 'agent10'
      }
      steps {
        // Send notifications about the build status
        sh 'slackSend channel: "#build-notifications", message: "Build successful!"'
        emailext body: "The build succeeded. Find more details at: ${BUILD_URL}", subject: 'Build Success', to: 'dev-team@example.com'

        // Notify via Gmail
        emailext (
          subject: 'Deployment Notification',
          body: 'The deployment was successful!',
          to: 'recipient@example.com',
          attachLog: true,
          replyTo: 'noreply@example.com',
          mimeType: 'text/html'
        )
      }
  }

  post {
    always {
      // SSH into the Terraform machine to destroy infrastructure
      script {
        sshCommand = "sshpass -p '${TERRAFORM_PASSWORD}' ssh -o StrictHostKeyChecking=no ${TERRAFORM_USERNAME}@${TERRAFORM_HOST}"
        sshExecute = { command -> sh "${sshCommand} '${command}'" }

        // Use SSH to execute commands on the Terraform machine for cleanup
        sshExecute 'cd /path/to/terraform'
        sshExecute 'terraform destroy -auto-approve'
      }
    }

    success {
      // Send success notification
      echo 'Deployment succeeded!'
    }

    failure {
      // Send failure notification
      echo 'Deployment failed!'
    }
  }
}
