pipeline {
  agent any

  environment {
    GOOGLE_APPLICATION_CREDENTIALS = credentials('gcp-service-account-key')
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
        // Use Terraform to provision infrastructure
        sh 'terraform init'
        sh 'terraform plan -out=tfplan'
        sh 'terraform apply -auto-approve tfplan'

        // // SSH into the Terraform machine
        // script {
        //   sshCommand = "sshpass -p '${TERRAFORM_PASSWORD}' ssh -o StrictHostKeyChecking=no ${TERRAFORM_USERNAME}@${TERRAFORM_HOST}"
        //   sshExecute = { command -> sh "${sshCommand} '${command}'" }

        //   // Use SSH to execute commands on the Terraform machine
        //   sshExecute 'cd /path/to/terraform'
        //   sshExecute 'terraform init'
        //   sshExecute 'terraform plan -out=tfplan'
        //   sshExecute 'terraform apply -auto-approve tfplan'
        // }
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
        // Deploy the application to Compute Engine
        sh 'gcloud compute instances create my-instance --zone=my-zone --project=my-project --image=my-image'
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
      // Clean up infrastructure
      sh 'terraform destroy -auto-approve'
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
