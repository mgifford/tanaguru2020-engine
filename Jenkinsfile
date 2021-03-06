def COLOR_MAP = [
    'SUCCESS': 'good', 
    'FAILURE': 'danger',
    'UNSTABLE': 'warning',
]

pipeline {
    agent any
    stages {
        stage('Build') {
            agent {
                    docker 'maven'
            }
            steps {
                sh 'mvn clean package -X'
                sh '''
                REST_VERSION=$(mvn -q \
                    -Dexec.executable=echo \
                    -Dexec.args='${project.version}' \
                    --non-recursive \
                    exec:exec)
                echo ${REST_VERSION} > version.txt
                '''
                stash name: 'tanaguru2020-rest', includes: 'tanaguru-rest/target/tanaguru2020-rest-*.tar.gz'
                stash name: 'version', includes: 'version.txt'
            }
        }

        stage('Build docker image') {
            when {
                anyOf{
                branch 'develop'
                branch 'master'
                }
            }
            steps {
                git(url: "https://github.com/Tanaguru/tanaguru2020-docker", branch: "master", credentialsId: "github-rcharre")
                unstash 'tanaguru2020-rest'
                unstash 'version'
                sh '''
                wget http://download-origin.cdn.mozilla.net/pub/firefox/releases/69.0/linux-x86_64/en-US/firefox-69.0.tar.bz2
                mv firefox-69.0.tar.bz2 tanaguru2020-rest/image/
                '''

                sh '''
                wget https://github.com/mozilla/geckodriver/releases/download/v0.21.0/geckodriver-v0.21.0-linux64.tar.gz
                mv geckodriver-v0.21.0-linux64.tar.gz tanaguru2020-rest/image/
                '''

                sh '''
                REST_VERSION=$(cat version.txt)
                mv tanaguru-rest/target/tanaguru2020-rest-*.tar.gz ./tanaguru2020-rest/image/tanaguru2020-rest-${REST_VERSION}.tar.gz
                docker build -t tanaguru2020-rest:${REST_VERSION} \
                    --build-arg TANAGURU_REST_ARCHIVE_PATH=tanaguru2020-rest-${REST_VERSION}.tar.gz \
                    --build-arg FIREFOX_ARCHIVE_PATH=firefox-69.0.tar.bz2 \
                    --build-arg GECKODRIVER_ARCHIVE_PATH=geckodriver-v0.21.0-linux64.tar.gz \
                    ./tanaguru2020-rest/image/
                '''
            }
        }

        stage('Deploy dev') {
            when {
                branch 'develop'
            }
            steps {
                unstash 'version'
                sh '''
                REST_VERSION=$(cat version.txt)

                echo SERVER_ADDRESS=0.0.0.0 > .env
                echo SERVER_PORT=9002 >> .env
                echo DB_URL=jdbc:postgresql://tanaguru2020-db-dev:5432/tanaguru >> .env
                echo DB_USERNAME=tanaguru >> .env
                echo DB_PASSWORD=tngTanaguru2020 >> .env
                echo MAIL_FROM_ADDRESS=support@tanaguru_com >> .env
                echo MAIL_HOST=localhost >> .env
                echo MAIL_PORT=587 >> .env
                echo MAIL_USERNAME= >> .env
                echo MAIL_PASSWORD= >> .env
                echo CRYPTO_KEY=change_Me_Please >> .env
                echo PASSWORD_TOKEN_VALIDITY=86400 >> .env
                echo AUDITRUNNER_PROXY_EXCLUSION_URLS= >> .env
                echo AUDITRUNNER_PROXY_USERNAME= >> .env
                echo AUDITRUNNER_PROXY_PASSWORD= >> .env
                echo AUDITRUNNER_PROXY_PORT= >> .env
                echo AUDITRUNNER_PROXY_HOST= >> .env
                echo AUDITRUNNER_IMPLICIT_WAIT=0 >> .env
                echo AUDITRUNNER_PAGE_LOAD_TIMEOUT=10 >> .env
                echo AUDITRUNNER_SCRIPT_TIMEOUT=10 >> .env
                echo AUDITRUNNER_FIREFOX_PROFILE= >> .env
                echo AUDITRUNNER_MAX_CONCURRENT_AUDITS=5 >> .env
                echo CORS_ORIGIN=* >> .env
                echo WEBAPP_URL=https://dev.tanaguru.com/#/ >> .env
                echo SESSION_TIMEOUT=1800 >> .env

                docker stop tanaguru2020-rest-dev || true
                docker image prune -f

                docker run -d --rm \
                    --name tanaguru2020-rest-dev \
                    --shm-size=2gb \
                    --env-file ./.env \
                    --label "traefik.enable=true" \
                    --label "traefik.frontend.redirect.entryPoint=secure" \
                    --label "traefik.http.routers.tanaguru-rest-dev.entrypoints=secure" \
                    --label "traefik.http.routers.tanaguru-rest-dev.rule=Host(\\`devapi.tanaguru.com\\`)" \
                    --label "traefik.http.routers.tanaguru-rest-dev.tls=true" \
                    --label "traefik.port=9002" \
                    --network=web \
                    tanaguru2020-rest:${REST_VERSION}
                '''
            }
        }

        stage('Deploy prod') {
            when {
                branch 'master'
            }
            steps {
                unstash 'version'
                sh '''
                REST_VERSION=$(cat version.txt)
                echo SERVER_ADDRESS=0.0.0.0 > .env
                echo SERVER_PORT=9002 >> .env
                echo DB_URL=jdbc:postgresql://tanaguru2020-db-prod:5432/tanaguru >> .env
                echo DB_USERNAME=tanaguru >> .env
                echo DB_PASSWORD=tngProd2020 >> .env
                echo MAIL_FROM_ADDRESS=support@tanaguru_com >> .env
                echo MAIL_HOST=localhost >> .env
                echo MAIL_PORT=587 >> .env
                echo MAIL_USERNAME= >> .env
                echo MAIL_PASSWORD= >> .env
                echo CRYPTO_KEY=tanaguruProd2020 >> .env
                echo PASSWORD_TOKEN_VALIDITY=86400 >> .env
                echo AUDITRUNNER_PROXY_EXCLUSION_URLS= >> .env
                echo AUDITRUNNER_PROXY_USERNAME= >> .env
                echo AUDITRUNNER_PROXY_PASSWORD= >> .env
                echo AUDITRUNNER_PROXY_PORT= >> .env
                echo AUDITRUNNER_PROXY_HOST= >> .env
                echo AUDITRUNNER_IMPLICIT_WAIT=0 >> .env
                echo AUDITRUNNER_PAGE_LOAD_TIMEOUT=10 >> .env
                echo AUDITRUNNER_SCRIPT_TIMEOUT=10 >> .env
                echo AUDITRUNNER_FIREFOX_PROFILE= >> .env
                echo AUDITRUNNER_MAX_CONCURRENT_AUDITS=5 >> .env
                echo CORS_ORIGIN=* >> .env
                echo WEBAPP_URL=https://prod.tanaguru.com/#/ >> .env
                echo SESSION_TIMEOUT=1800 >> .env

                docker stop tanaguru2020-rest-prod || true
                docker image prune -f
                
                docker run -d --rm \
                    --name tanaguru2020-rest-prod \
                    --shm-size=2gb \
                    --env-file ./.env \
                    --label "traefik.enable=true" \
                    --label "traefik.frontend.redirect.entryPoint=secure" \
                    --label "traefik.http.routers.tanaguru-rest-prod.entrypoints=secure" \
                    --label "traefik.http.routers.tanaguru-rest-prod.rule=Host(`prodapi.tanaguru.com`)" \
                    --label "traefik.http.routers.tanaguru-rest-prod.tls=true" \
                    --label "traefik.port=9002" \
                    --network=web \
                    tanaguru2020-rest:${REST_VERSION}
                '''
            }
        }

        stage('Store packages') {
            when {
                branch 'master'
            }
            steps {
                unstash 'tanaguru2020-rest'
                unstash 'version'

                sh '''
                    REST_VERSION=$(cat version.txt)
                    mkdir -p /html/tanaguru2020-rest/${REST_VERSION}
                    mv -f tanaguru-rest/target/tanaguru2020-rest-*.tar.gz /html/tanaguru2020-rest/${REST_VERSION}/tanaguru2020-rest-${REST_VERSION}.tar.gz
                    chown 1000:1000 /html/tanaguru2020-rest/${REST_VERSION}/tanaguru2020-rest-${REST_VERSION}.tar.gz
                '''
            }
        }

        stage('Push image to registry') {
            environment {
                REGISTRY_USER = "admin"
                REGISTRY_PASS = "9x^VTugHEfQ1e7"
                REGISTRY_HOST = "registry.tanaguru.com"
            }
            when {
                branch 'master'
            }
            steps {
                unstash 'version'

                sh '''
                REST_VERSION=$(cat version.txt)

                docker login \
                --username="$REGISTRY_USER" \
                --password="$REGISTRY_PASS" "$REGISTRY_HOST"
                docker tag tanaguru2020-rest:${REST_VERSION} registry.tanaguru.com/tanaguru2020-rest:${REST_VERSION}
                docker push registry.tanaguru.com/tanaguru2020-rest:${REST_VERSION}

                docker tag tanaguru2020-rest:${REST_VERSION} registry.tanaguru.com/tanaguru2020-rest:latest
                docker push registry.tanaguru.com/tanaguru2020-rest:latest
                '''
            }
        }
    }

    post {
        always {
            slackSend channel: '#jenkins',
                color: COLOR_MAP[currentBuild.currentResult],
                message: "*${currentBuild.currentResult}:* Job ${env.JOB_NAME} build ${env.BUILD_NUMBER}\nMore info at: ${env.BUILD_URL}"
        }
    }
}

