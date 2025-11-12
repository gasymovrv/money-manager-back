# Telegram Integration Setup Guide

This guide explains how to set up Telegram bot integration for the Money Manager application.

## Overview

The integration consists of two main features:
1. **Telegram Login Widget** - Allows users to link their Telegram account to Money Manager
2. **Telegram Webhook** - Receives messages from users via Telegram bot

## Prerequisites

1. Create a Telegram bot using [@BotFather](https://t.me/botfather)
2. Get your bot token and username

## Step 1: Create a Telegram Bot

1. Open Telegram and search for [@BotFather](https://t.me/botfather)
2. Send `/newbot` command
3. Follow the instructions to create your bot
4. Save the **bot token** you receive
5. Save the **bot username** (e.g., `YourMoneyManagerBot`)

## Step 2: Configure Bot Domain

1. Send `/setdomain` command to @BotFather
2. Select your bot
3. Enter your domain (e.g., `localhost` for development or `yourdomain.com` for production)

## Step 3: Configure Backend

1. Open `money-manager-back/.env` file
2. Set the following variables:
   ```env
   TELEGRAM_BOT_TOKEN=your-bot-token-here
   ```

3. The backend configuration is already set up in `application.yml`:
   ```yaml
   telegram:
     bot:
       token: ${TELEGRAM_BOT_TOKEN:your-bot-token-here}
   ```

## Step 4: Configure Frontend

1. Open `money-manager-front/.env` file
2. Set the bot username:
   ```env
   REACT_APP_TELEGRAM_BOT_USERNAME=your-bot-username
   ```

## Step 5: Run Database Migrations

The Liquibase migrations will automatically create the required tables:
- `telegram_users` - Stores Telegram user mappings
- `telegram_messages` - Stores processed messages for deduplication

Just start the backend application and the migrations will run automatically.

## Step 6: Set Up Webhook

### Generate Webhook Secret (Recommended)

For security, generate a random secret token:

```bash
# Generate a random secret (Linux/Mac)
openssl rand -hex 32

# Or use any random string generator
```

Add it to `money-manager-back/.env`:
```env
TELEGRAM_WEBHOOK_SECRET=your-generated-secret-here
```

### Set Webhook URL

After deploying your application, set the webhook URL with the secret token:

```bash
curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{ 
    "url": "https://your-domain.com/api/telegram/webhook",
    "secret_token": "your-generated-secret-here"
  }'
```

**Note:** The webhook endpoint verifies the `X-Telegram-Bot-Api-Secret-Token` header matches your configured secret.

## How It Works

### Login Widget

1. After logging into Money Manager, users will see a Telegram login button in the header
2. Clicking it opens Telegram authentication
3. After authentication, the Telegram account is linked to the Money Manager user
4. The backend verifies the authentication using HMAC-SHA256 signature

### Webhook

1. Users send messages to your bot in Telegram
2. Telegram sends updates to your webhook endpoint
3. The backend:
   - Checks if the message was already processed (deduplication)
   - Verifies the user is linked to Money Manager
   - Logs the message (currently just logging, ready for future processing)
   - Saves the message ID to prevent duplicate processing

## API Endpoints

### POST /api/telegram/link
Links a Telegram account to the current Money Manager user.

**Request Body:**
```json
{
  "id": 123456789,
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "photoUrl": "https://...",
  "authDate": 1699999999,
  "hash": "abc123..."
}
```

### POST /api/telegram/webhook
Receives webhook updates from Telegram (called by Telegram, not by frontend).

**Request Body:** Standard Telegram Update object

## Database Schema

### telegram_users
- `telegram_id` (PK) - Telegram user ID
- `user_id` (FK) - Money Manager user ID
- `first_name` - User's first name
- `last_name` - User's last name
- `username` - Telegram username
- `photo_url` - Profile photo URL
- `auth_date` - Authentication timestamp
- `created_at` - Record creation timestamp
- `updated_at` - Record update timestamp

### telegram_messages
- `message_id` (PK) - Telegram message ID
- `telegram_id` - Telegram user ID
- `chat_id` - Chat ID
- `message_text` - Message content
- `processed_at` - Processing timestamp

## Security

1. **Webhook Secret Token**: The webhook endpoint verifies `X-Telegram-Bot-Api-Secret-Token` header to ensure requests come from Telegram
2. **Authentication Verification**: The backend verifies Telegram login widget data using HMAC-SHA256 with the bot token
3. **User Linking**: Only authenticated Money Manager users can link their Telegram accounts
4. **Message Authorization**: Webhook only processes messages from linked users
5. **Deduplication**: Message IDs are stored to prevent duplicate processing

## Local Testing

**Important:** Telegram Login Widget requires the page to be served from the exact domain you set in BotFather. Using `localhost:3000` won't work due to CSP restrictions.

### Using Local Nginx

This setup uses nginx in Docker to proxy both frontend and backend through one domain.

#### Setup:

1. **Start nginx via Docker Compose:**
   ```bash
   cd money-manager-back
   docker-compose -f docker-compose-local.yml up -d
   ```
   Nginx will proxy requests from `http://mm.localtest.me` to your local ports.

2. Use ngrok to expose your backend:
   ```bash
   ngrok http http://mm.localtest.me
   ```
   You'll get a URL like: `https://abc123.ngrok-free.dev`

3. **Configure environment variables:**

   `money-manager-back/.env`:
   ```env
   TELEGRAM_BOT_TOKEN=your-bot-token-here
   ALLOWED_ORIGINS=https://abc123.ngrok-free.dev
   ```

   `money-manager-front/.env`:
   ```env
   REACT_APP_BACKEND_HOST=https://abc123.ngrok-free.dev
   REACT_APP_TELEGRAM_BOT_USERNAME=your-bot-username
   ```
   
    Don't forget add ngrok url as redirect_uri and allowed domain to OAuth providers (VK and Google)

4. **Start backend in IDE:**
   ```bash
   cd money-manager-back
   mvn spring-boot:run
   ```
   Backend will run on `localhost:8080`

5. **Start frontend in IDE:**
   ```bash
   cd money-manager-front
   npm start
   ```
   Frontend will run on `localhost:3000`

6. **Set domain in BotFather:**
   - Open @BotFather in Telegram
   - Send `/setdomain`
   - Select your bot
   - Enter: `abc123.ngrok-free.dev`

7. **Set up webhook for receiving messages:**

   Set webhook:
   ```bash
   curl -X POST "https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook" \
   -H "Content-Type: application/json" \
   -d '{
    "url": "https://abc123.ngrok-free.dev/api/telegram/webhook",
    "secret_token": "your-generated-secret-here"
   }'
   ```

8. **Open application:**
   Open in browser: `https://abc123.ngrok-free.dev`

9. **Test Login Widget:**
   - Login to Money Manager
   - Click Telegram widget in header
   - Authenticate with Telegram
   - Check backend logs for successful account linking
   - Send a message to your bot in Telegram and check backend logs.

#### Stop nginx:
```bash
cd money-manager-back
docker-compose -f docker-compose-local.yml down
```

## Testing

1. Start the backend and frontend applications
2. Log in to Money Manager
3. Click the Telegram login widget in the header
4. Authenticate with Telegram
5. Send a message to your bot in Telegram
6. Check the backend logs to see the message was received and processed

## Troubleshooting

### Widget not appearing
- Check that `REACT_APP_TELEGRAM_BOT_USERNAME` is set correctly
- Ensure you're logged in to Money Manager
- Check browser console for errors

### Authentication fails
- Verify `TELEGRAM_BOT_TOKEN` is correct
- Check that domain is set correctly in BotFather
- Ensure auth_date is not too old (max 24 hours)

### Webhook not receiving messages
- Verify webhook URL is set correctly
- Check that webhook URL is publicly accessible
- Ensure bot token is correct
- Check backend logs for errors

## Future Enhancements

Currently, the webhook only logs messages. You can extend it to:
- Parse commands (e.g., `/add_expense 100 Food`)
- Send responses back to users
- Query account balance
- Generate reports
- Set up notifications

## References

- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Telegram Login Widget](https://core.telegram.org/widgets/login)
- [Telegram Webhooks](https://core.telegram.org/bots/api#setwebhook)
