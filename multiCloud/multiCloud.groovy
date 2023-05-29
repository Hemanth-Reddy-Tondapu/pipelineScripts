pipeline {
  agent any

  environment {
    AWS_ACCESS_KEY_ID = credentials('aws-access-key')
    AWS_SECRET_ACCESS_KEY = credentials('aws-secret-key')
    GCP_SERVICE_ACCOUNT_KEY = credentials('gcp-service-account-key')
    KUBECONFIG = credentials('kubeconfig')
    TF_VAR_project_id = 'my-project'
    TF_VAR_region = 'us-central1'
    TF_VAR_vpc_cidr = '10.0.0.0/16'
    TF_VAR_subnet_cidr = '10.0.1.0/24'
    TF_VAR_cluster_name = 'my-cluster'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Provision Infrastructure') {
      parallel {
        stage('Provision AWS Infrastructure') {
          agent {
            docker {
              image 'hashicorp/terraform:1.1.0'
              args '-v $HOME/.terraform.d/plugins:/root/.terraform.d/plugins'
            }
          }
          environment {
            TF_PLUGIN_CACHE_DIR = '$HOME/.terraform.d/plugin-cache'
          }
          steps {
            sh 'terraform init -plugin-dir=$HOME/.terraform.d/plugins infra/aws'
            sh 'terraform validate infra/aws'
            sh 'terraform plan -out=tfplan infra/aws'
            sh 'terraform apply -auto-approve tfplan infra/aws'
          }
        }
        stage('Provision GCP Infrastructure') {
          agent {
            docker {
              image 'hashicorp/terraform:1.1.0'
              args '-v $HOME/.terraform.d/plugins:/root/.terraform.d/plugins'
            }
          }
          environment {
            TF_PLUGIN_CACHE_DIR = '$HOME/.terraform.d/plugin-cache'
          }
          steps {
            sh 'terraform init -plugin-dir=$HOME/.terraform.d/plugins infra/gcp'
            sh 'terraform validate infra/gcp'
            sh 'terraform plan -out=tfplan infra/gcp'
            sh 'terraform apply -auto-approve tfplan infra/gcp'
          }
        }
      }
    }

    stage('Build and Push Docker Image') {
      agent {
        docker {
          image 'docker:20.10.9'
          reuseNode true
        }
      }
      steps {
        sh 'docker build -t myapp:${BUILD_NUMBER} .'
        sh 'docker push myapp:${BUILD_NUMBER}'
      }
    }

    stage('Deploy to Kubernetes Cluster') {
      agent {
        kubernetes {
          kubeconfigId 'kubeconfig'
          defaultContainer 'kubectl'
        }
      }
      steps {
        container('kubectl') {
          sh 'kubectl apply -f kubernetes/deployment.yaml'
          sh 'kubectl apply -f kubernetes/service.yaml'
        }
      }
    }

    stage('Deploy to AWS') {
      agent {
        docker {
          image 'amazon/aws-cli:2.4.0'
          args '-v $HOME/.aws:/root/.aws'
        }
      }
      steps {
        withCredentials([file(credentialsId: 'aws-access-key', variable: 'AWS_ACCESS_KEY_ID'),
                         file(credentialsId: 'aws-secret-key', variable: 'AWS_SECRET_ACCESS_KEY')]) {
          sh 'aws eks update-kubeconfig --region us-west-2 --name my-cluster'
          sh 'kubectl apply -f kubernetes/aws-configmap.yaml'
        }
      }
    }

    stage('Deploy to GCP') {
      agent {
        docker {
          image 'google/cloud-sdk:358.0.0'
          args '-v $HOME/.config/gcloud:/root/.config/gcloud'
        }
      }
      steps {
        withCredentials([file(credentialsId: 'gcp-service-account-key', variable: 'GCP_SERVICE_ACCOUNT_KEY')]) {
          sh 'gcloud auth activate-service-account --key-file=$GCP_SERVICE_ACCOUNT_KEY'
          sh 'gcloud config set project my-project'
          sh 'gcloud container clusters get-credentials my-cluster --zone us-central1'
          sh 'kubectl apply -f kubernetes/gcp-configmap.yaml'
        }
      }
    }
  }

  post {
    always {
      echo 'Pipeline completed'
    }
    success {
      echo 'Pipeline succeeded'
    }
    failure {
      echo 'Pipeline failed'
    }
    catchError {
      echo 'Error occurred'
      currentBuild.result = 'FAILURE'
    }
  }
}
