FROM nginx:alpine

# Create directory for SSL certificates
RUN mkdir -p /etc/nginx/ssl

# Copy Nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# The SSL certificates will be mounted at runtime 