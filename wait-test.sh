# Wait until the app answers, so that "ready" really means ready (up to 5 minutes)
echo "Waiting for the app to start..."
for i in $(seq 1 60); do
  if curl -sf -o /dev/null http://localhost:8080/; then
    echo
    echo "Ready: the app is running on port 8080."
    echo "The browser opens automatically; otherwise open port 8080 from the Ports tab."
    exit 0
  fi
  sleep 2
done

echo
echo "The app did not answer within 5 minutes. Check the logs:"
echo "  cd onboarding-progress-system && docker compose logs --tail 50 app"
exit 1
