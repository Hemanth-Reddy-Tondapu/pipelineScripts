pipeline {
  agent any
  
  stages {
    stage('Checkout') {
      steps {
        // Checkout your source code repository
        git 'https://github.com/your/repo.git'
      }
    }
    
    stage('Build') {
      steps {
        // Build your Express application
        sh 'npm install'
        sh 'npm run build'
      }
    }
    
    stage('Terraform Provisioning') {
      steps {
        // Provision infrastructure using Terraform
        withCredentials([file(credentialsId: 'terraform-credentials', variable: 'TF_CREDS_FILE')]) {
          sh 'terraform init -backend-config="bucket=my-bucket"'
          sh 'terraform plan -out=tfplan -input=false'
          sh 'terraform apply -input=false tfplan'
        }
      }
    }
    
    stage('Deploy to EC2') {
      steps {
        // Deploy to EC2 instance
        // Copy the build artifacts to EC2
        sh 'scp -i /path/to/private_key.pem -r ./dist ubuntu@ec2-instance:/var/www'
        
        // Start your Express application on EC2
        sshagent(['/path/to/private_key.pem']) {
          sh 'ssh -i /path/to/private_key.pem ubuntu@ec2-instance "cd /var/www && npm install && npm start"'
        }
      }
    }
    
    stage('Deploy to Elastic Beanstalk') {
      steps {
        // Deploy to Elastic Beanstalk
        withCredentials([string(credentialsId: 'aws-credentials', variable: 'AWS_CREDENTIALS')]) {
          // Initialize Elastic Beanstalk environment
          sh 'eb init -r us-east-1 -p node.js'
          
          // Deploy application to Elastic Beanstalk
          sh 'eb deploy'
          
          // Wait for deployment to finish
          sh 'eb status'
        }
      }
    }
    
    stage('Deploy to Lambda') {
      steps {
        // Deploy to Lambda
        withCredentials([string(credentialsId: 'aws-credentials', variable: 'AWS_CREDENTIALS')]) {
          // Package your Express application
          sh 'zip -r lambda-deployment-package.zip ./dist'
          
          // Create a Lambda function using AWS CLI
          sh 'aws lambda create-function --function-name my-lambda-function --runtime nodejs14.x --role arn:aws:iam::1234567890:role/lambda-role --handler index.handler --zip-file fileb://lambda-deployment-package.zip'
          
          // Or, use Terraform to create the Lambda function
          // sh 'terraform apply'
        }
      }
    }
    
    stage('Deploy to App Runner') {
      steps {
        // Deploy to App Runner
        // Build a container image of your Express application
        sh 'docker build -t my-app-runner-image ./dist'
        
        // Push the container image to a container registry (e.g., Amazon ECR)
        sh 'docker tag my-app-runner-image:latest aws_account_id.dkr.ecr.us-east-1.amazonaws.com/my-app-runner-image:latest'
        sh 'docker push aws_account_id.dkr.ecr.us-east-1.amazonaws.com/my-app-runner-image:latest'
        
        // Deploy the container image to App Runner
        sh 'aws apprunner create-service --service-name my-app-runner-service --source-configuration imageRepository.type=ECR,imageRepository.repositoryUrl=aws_account_id.dkr.ecr.us-east-1.amazonaws.com/my-app-runner-image,imageConfiguration.port=3000'
      }
    }
    
    stage('Deploy to Fargate') {
      steps {
        // Deploy to Fargate
        // Build a container image of your Express application
        sh 'docker build -t my-fargate-image ./dist'
        
        // Push the container image to a container registry (e.g., Amazon ECR)
        sh 'docker tag my-fargate-image:latest aws_account_id.dkr.ecr.us-east-1.amazonaws.com/my-fargate-image:latest'
        sh 'docker push aws_account_id.dkr.ecr.us-east-1.amazonaws.com/my-fargate-image:latest'
        
        // Use ECS/EKS/Fargate service or Terraform to deploy the container image to Fargate
        // Replace the placeholders with the appropriate commands or Terraform configurations
      }
    }
    
    stage('Notifications') {
      steps {
        // Send notification to Slack channel
        slackSend(channel: '#your-slack-channel', message: 'Application deployment successful!')
        
        // Send email notification
        emailext (
          subject: 'Application Deployment',
          body: 'Application successfully deployed to AWS services.',
          to: 'your-email@example.com',
          from: 'jenkins@example.com'
        )
      }
    }
  }
  
  post {
    always {
      // Cleanup and tear down infrastructure
      // Execute Terraform destroy or other cleanup steps
      // This ensures cleanup is performed even if the pipeline fails
      withCredentials([file(credentialsId: 'terraform-credentials', variable: 'TF_CREDS_FILE')]) {
        sh 'terraform destroy -auto-approve'
      }
      
      // Remove Docker images or other cleanup steps
      sh 'docker rmi my-app-image:latest'

      // Cleanup for EC2 instances
      withCredentials([string(credentialsId: 'aws-credentials', variable: 'AWS_CREDENTIALS')]) {
        // Terminate EC2 instances
        sh 'aws ec2 terminate-instances --instance-ids i-1234567890abcdef0'
      }

      // Cleanup for Elastic Beanstalk
      withCredentials([string(credentialsId: 'aws-credentials', variable: 'AWS_CREDENTIALS')]) {
        // Delete Elastic Beanstalk environment
        sh 'aws elasticbeanstalk terminate-environment --environment-name my-environment'
        
        // Delete Elastic Beanstalk application version
        sh 'aws elasticbeanstalk delete-application-version --application-name my-app --version-label v1.0.0'
      }
      
      // Example cleanup for Lambda
      withCredentials([string(credentialsId: 'aws-credentials', variable: 'AWS_CREDENTIALS')]) {
        sh 'aws lambda delete-function --function-name my-lambda-function'
      }
      
      // Example cleanup for App Runner
      sh 'aws apprunner delete-service --service-name my-app-runner-service'
      
      // Cleanup for Fargate
      // Use ECS/EKS/Fargate service or Terraform to remove the deployed container
      // For example, using AWS CLI to delete the Fargate service and task definition
      sh 'aws ecs delete-service --cluster my-cluster --service my-fargate-service --force'
      sh 'aws ecs deregister-task-definition --task-definition my-fargate-task-definition'
      
      // Additional cleanup steps as needed
    }
    
    success {
      // Perform any success-specific actions
      
      // Send success notification to Slack channel
      slackSend(channel: '#your-slack-channel', message: 'Application deployment successful!')
      
      // Send success email notification
      emailext (
        subject: 'Application Deployment Successful',
        body: 'Application successfully deployed to AWS services.',
        to: 'your-email@example.com',
        from: 'jenkins@example.com'
      )
    }
    
    failure {
      // Perform any failure-specific actions
      
      // Send failure notification to Slack channel
      slackSend(channel: '#your-slack-channel', message: 'Application deployment failed!')
      
      // Send failure email notification
      emailext (
        subject: 'Application Deployment Failed',
        body: 'Application deployment to AWS services failed.',
        to: 'your-email@example.com',
        from: 'jenkins@example.com'
      )
    }
  }
}
