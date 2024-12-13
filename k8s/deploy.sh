helm install mm-config ./mm-config-chart
helm install -f values-postgres.yaml mm-pstgr ./mm-app-chart
helm install -f values-backend.yaml mm-back ./mm-app-chart
helm install -f values-frontend.yaml mm-front ./mm-app-chart
