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
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }

       stage('Deploy') {
            steps {
                sh '''
                cp target/super-admin-0.0.1-SNAPSHOT.jar /opt/super-admin/app.jar
                sudo pm2 restart super-admin
                '''
            }
}

    }
}