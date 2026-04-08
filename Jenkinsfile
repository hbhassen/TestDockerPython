pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B clean compile'
                    } else {
                        bat 'mvn -B clean compile'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B test'
                    } else {
                        bat 'mvn -B test'
                    }
                }
            }
        }

        stage('Package') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'mvn -B package -DskipTests'
                    } else {
                        bat 'mvn -B package -DskipTests'
                    }
                }
            }
        }

        stage('Docker build') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker build -t enterprise-app-h2:latest .'
                    } else {
                        bat 'docker build -t enterprise-app-h2:latest .'
                    }
                }
            }
        }
    }
}
