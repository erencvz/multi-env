// Shared library: retry wrapper with configurable delay.
// Used by all Apinizer Sync and Smoke Test stages in the CD pipeline.

def call(int maxAttempts, int delaySeconds, String stageName, Closure body) {
    int attempt = 0
    while (true) {
        attempt++
        try {
            echo "[${stageName}] Attempt ${attempt} of ${maxAttempts}..."
            body()
            echo "[${stageName}] Succeeded on attempt ${attempt}."
            return
        } catch (Exception e) {
            if (attempt >= maxAttempts) {
                echo "[${stageName}] All ${maxAttempts} attempts failed. Last error: ${e.message}"
                throw e
            }
            echo "[${stageName}] Attempt ${attempt} failed: ${e.message}"
            echo "[${stageName}] Retrying in ${delaySeconds}s... (${attempt}/${maxAttempts})"
            sleep(delaySeconds)
        }
    }
}
