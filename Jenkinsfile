pipeline {
    agent any
    options {
        skipStagesAfterUnstable()
    }
    stages {
        stage('Build') {
            steps {
                echo 'Building'x
            }
            if (env.BRANCH_NAME == 'master') {
                echo 'I only execute on the master branch'
            } else {
                echo 'I execute elsewhere'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying'
            }
        }
    }
}
