pipeline {
  agent any

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
      steps {
        // Checkout source code from version control system
        git 'https://github.com/example/repo.git'
      }
    }

    stage('Build') {
      steps {
        // Install dependencies and build the Express application
        sh 'npm ci'
        sh 'npm run build'
      }
    }

    stage('Test') {
      steps {
        // Run unit tests
        sh 'npm run test'
      }
    }

    stage('Provision Infrastructure') {
      steps {
        // SSH into the Terraform machine
        script {
          sshCommand = "sshpass -p '${TERRAFORM_PASSWORD}' ssh -o StrictHostKeyChecking=no ${TERRAFORM_USERNAME}@${TERRAFORM_HOST}"
          sshExecute = { command -> sh "${sshCommand} '${command}'" }

          // Use SSH to execute commands on the Terraform machine
          sshExecute 'cd /path/to/terraform'
          sshExecute 'terraform init'
          sshExecute 'terraform plan -out=tfplan'
          sshExecute 'terraform apply -auto-approve tfplan'
        }
      }
    }

    stage('Deploy to Google App Engine') {
      when {
        branch 'master'
      }
      steps {
        // Deploy the application to Google App Engine
        sh 'gcloud app deploy --project=my-project --version=my-version app.yaml'
      }
    }

    stage('Deploy to Google Kubernetes Engine (GKE)') {
      when {
        branch 'master'
      }
      steps {
        // Authenticate with GKE cluster
        sh 'gcloud container clusters get-credentials my-cluster --zone=my-zone --project=my-project'

        // Deploy the application to GKE
        sh 'kubectl apply -f kubernetes-deployment.yaml'
      }
    }

    stage('Deploy to Compute Engine') {
      when {
        branch 'master'
      }
      steps {
        // Copy application files to Compute Engine instance
        sh "gcloud compute scp --project=${COMPUTE_PROJECT} --zone=${COMPUTE_ZONE} app.zip ${COMPUTE_INSTANCE_NAME}:~"

        // SSH into Compute Engine instance and deploy the application
        sh "gcloud compute ssh --project=${COMPUTE_PROJECT} --zone=${COMPUTE_ZONE} ${COMPUTE_INSTANCE_NAME} --command='unzip app.zip && cd app && ./deploy.sh'"
      }
    }

    stage('Deploy to Cloud Run') {
      when {
        branch 'master'
      }
      steps {
        // Deploy the application to Cloud Run
        sh 'gcloud run deploy my-service --image=my-image --platform=managed --project=my-project --region=my-region'
      }
    }

    stage('Deploy to Cloud Functions') {
      when {
        branch 'master'
      }
      steps {
        // Deploy the application to Cloud Functions
        sh 'gcloud functions deploy my-function --runtime=nodejs14 --trigger-http --allow-unauthenticated --project=my-project'
      }
    }

    stage('Deploy to Firebase Hosting') {
      when {
        branch 'master'
      }
      steps {
        // Deploy the application to Firebase Hosting
        sh 'firebase deploy --project=my-project'
      }
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


/***
 * Please note that you need to replace placeholders such as 
    my-project,my-version, my-cluster, my-zone, my-image, my-region, etc., 
    with your actual project and resource names.
 *  Make sure to replace /path/to/terraform with the actual path to your Terraform files on the remote machine.
 */



