// Shared library: Apinizer API proxy create-or-update + deploy.
// Used by Apinizer Sync stages (dev, test, uat) in the CD pipeline.
// Flow: check proxy existence → create or (snapshot + update) → deploy to environment.

def call(Map args) {
    def proxyName   = args.proxyName
    def projectName = args.projectName
    def openApiUrl  = args.openApiUrl
    def environment = args.environment
    def versionTag  = args.versionTag
    def baseUrl     = env.APINIZER_URL
    def token       = env.APINIZER_TOKEN
    def backendUrl  = openApiUrl.replace('/openapi.json', '')

    echo "Checking if API proxy exists: ${projectName}/${proxyName}"
    def proxyCheckCode = sh(
        script: '''
            curl -s -o /tmp/apinizer_get.json -w '%{http_code}' \
                -H 'Authorization: Bearer ''' + token + '''' \
                \'''' + baseUrl + '''/apiops/projects/''' + projectName + '''/apiProxies/''' + proxyName + '''/\'
        ''',
        returnStdout: true
    ).trim()
    echo "Proxy check returned: ${proxyCheckCode}"

    def proxyExists = (proxyCheckCode == '200')

    if (proxyExists) {
        echo 'API proxy exists — taking rollback snapshot and updating spec...'

        sh('''
            curl -s -o apinizer_backup_''' + environment + '_' + proxyName + '''.zip \
                -H 'Authorization: Bearer ''' + token + '''' \
                \'''' + baseUrl + '''/apiops/projects/''' + projectName + '''/apiProxies/''' + proxyName + '''/export/\'
            echo 'Rollback snapshot taken.'
        ''')
        archiveArtifacts artifacts: "apinizer_backup_${environment}_${proxyName}.zip",
                         allowEmptyArchive: true

        def proxyJson        = readJSON file: '/tmp/apinizer_get.json'
        def relativePathList = proxyJson.resultList[0].clientRoute.relativePathList
        def relativePathJson = '[' + relativePathList.collect { '"' + it + '"' }.join(',') + ']'
        echo "Existing relativePathList: ${relativePathJson}"

        def updateStatus = sh(
            script: '''
                curl -s -o /tmp/apinizer_update.json -w '%{http_code}' \
                    -X PUT \
                    -H 'Authorization: Bearer ''' + token + '''' \
                    -H 'Content-Type: application/json' \
                    -d '{
                        "apiProxyName": "''' + proxyName + '''",
                        "backendApiVersion": "''' + versionTag + '''",
                        "apiProxyCreationType": "OPEN_API",
                        "specUrl": "''' + openApiUrl + '''",
                        "clientRoute": {
                            "relativePathList": ''' + relativePathJson + ''',
                            "hostList": []
                        },
                        "reParse": true,
                        "deploy": false
                    }' \
                    \'''' + baseUrl + '''/apiops/projects/''' + projectName + '''/apiProxies/url/\'
            ''',
            returnStdout: true
        ).trim()

        echo "Apinizer update HTTP: ${updateStatus}"
        if (!updateStatus.startsWith('2')) {
            error("Proxy update failed (HTTP ${updateStatus}): ${readFile('/tmp/apinizer_update.json')}")
        }

    } else {
        echo 'API proxy not found — creating...'

        def createStatus = sh(
            script: '''
                curl -s -o /tmp/apinizer_create.json -w '%{http_code}' \
                    -X POST \
                    -H 'Authorization: Bearer ''' + token + '''' \
                    -H 'Content-Type: application/json' \
                    -d '{
                        "apiProxyName": "''' + proxyName + '''",
                        "backendApiVersion": "''' + versionTag + '''",
                        "apiProxyDescription": "Auto-generated proxy",
                        "apiProxyCreationType": "OPEN_API",
                        "specUrl": "''' + openApiUrl + '''",
                        "clientRoute": {
                            "relativePathList": ["/''' + proxyName + '''"],
                            "hostList": []
                        },
                        "routingInfo": {
                            "routingAddressList": [
                                {
                                    "address": "''' + backendUrl + '''",
                                    "weight": 100,
                                    "healthCheckEnabled": false
                                }
                            ],
                            "routingEnabled": true,
                            "mirrorEnabled": false
                        },
                        "reParse": false,
                        "deploy": false
                    }' \
                    \'''' + baseUrl + '''/apiops/projects/''' + projectName + '''/apiProxies/url/\'
            ''',
            returnStdout: true
        ).trim()

        echo "Apinizer create HTTP: ${createStatus}"
        if (!createStatus.startsWith('2')) {
            error("Proxy creation failed (HTTP ${createStatus}): ${readFile('/tmp/apinizer_create.json')}")
        }
    }

    echo "Deploying ${projectName}/${proxyName} to environment: ${environment}"
    def deployStatus = sh(
        script: '''
            curl -s -o /tmp/apinizer_deploy.json -w '%{http_code}' \
                -X POST \
                -H 'Authorization: Bearer ''' + token + '''' \
                \'''' + baseUrl + '''/apiops/projects/''' + projectName + '''/apiProxies/''' + proxyName + '''/environments/''' + environment + '''/\'
        ''',
        returnStdout: true
    ).trim()

    echo "Apinizer deploy HTTP: ${deployStatus}"
    if (!deployStatus.startsWith('2')) {
        error("Apinizer deploy failed (HTTP ${deployStatus}): ${readFile('/tmp/apinizer_deploy.json')}")
    }
    echo "Proxy '${proxyName}' successfully deployed to ${environment}."
}
