// Shared library: Apinizer promotion endpoint wrapper.
// Promotes API proxies from a source environment (e.g. UAT) to production
// via the /apiops/promotion/executions endpoint.
// Used by the 'Apinizer Promote Prod' stage in the CD pipeline.

def call(Map args) {
    def mappingNames           = args.mappingNames           // List<String> — e.g. ['messagingapi']
    def executionName          = args.executionName          // String — display name for this promotion run
    def executionDescription   = args.executionDescription   // String — free-text description
    def baseUrl                = env.APINIZER_URL
    def token                  = env.APINIZER_TOKEN

    def mappingNamesJson = '[' + mappingNames.collect { '"' + it + '"' }.join(',') + ']'

    def payload = """{
  "mappingNames": ${mappingNamesJson},
  "executionName": "${executionName}",
  "executionDescription": "${executionDescription}"
}"""

    writeFile file: '/tmp/apinizer_promote_payload.json', text: payload
    echo "Promoting to PROD — mappings: ${mappingNames}, execution: ${executionName}"

    def promoteStatus = sh(
        script: """
            curl -s -o /tmp/apinizer_promote.json -w '%{http_code}' \\
                -X POST \\
                -H 'Authorization: Bearer ${token}' \\
                -H 'Content-Type: application/json' \\
                -d @/tmp/apinizer_promote_payload.json \\
                '${baseUrl}/apiops/promotion/executions'
        """,
        returnStdout: true
    ).trim()

    echo "Apinizer promote HTTP: ${promoteStatus}"

    if (!promoteStatus.startsWith('2')) {
        error("Prod promote failed (HTTP ${promoteStatus}): ${readFile('/tmp/apinizer_promote.json')}")
    }

    echo "Promotion completed successfully — execution: ${executionName}"
}