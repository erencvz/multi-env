// Shared library: Kubernetes rollout check + /health endpoint smoke test.
// Used by all Smoke Test stages in the CD pipeline.

def call(String namespace, String nodePort) {
    withEnv(["SMOKE_NAMESPACE=${namespace}", "SMOKE_PORT=${nodePort}"]) {
        sh '''
            export KUBECONFIG=$KUBECONFIG
            echo "Smoke test: http://$NODE_IP:${SMOKE_PORT}/health"

            kubectl rollout status deployment/multi-env-api \
                -n ${SMOKE_NAMESPACE} \
                --timeout=120s

            echo "Waiting 5 seconds for service to become ready..."
            sleep 5

            for i in 1 2 3; do
                HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
                    --connect-timeout 10 \
                    "http://$NODE_IP:${SMOKE_PORT}/health")
                echo "Attempt $i: HTTP ${HTTP_STATUS}"
                if [ "$HTTP_STATUS" = "200" ]; then
                    echo "/health 200 OK — ${SMOKE_NAMESPACE}"
                    exit 0
                fi
                sleep 5
            done

            echo "/health check failed with status: ${HTTP_STATUS} (expected: 200)"
            exit 1
        '''
    }
}
