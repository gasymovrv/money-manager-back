# Telegram Report Generation Feature

## Overview
This feature allows users to request financial reports via Telegram bot commands. The system uses a reliable queue mechanism with configurable retry logic to handle report generation asynchronously.

## Architecture

### Components

1. **Database Tables**
   - `report_tasks` - Stores report generation tasks with retry information
   - `telegram_user_states` - Tracks conversation state for each user

2. **Entities**
   - `ReportTask` - Represents a report generation task
   - `TelegramUserState` - Tracks user conversation flow state

3. **Services**
   - `TelegramService` - Handles webhook messages, commands, and date validation
   - `TelegramBotClient` - Client for Telegram Bot API (sending messages and files)
   - `ReportGenerationService` - Generates financial reports (stub implementation)
   - `ReportTaskProcessor` - Scheduled processor with retry logic

4. **Repositories**
   - `ReportTaskRepository` - Data access for report tasks
   - `TelegramUserStateRepository` - Data access for user states

## User Flow

### Step 1: Request Report
User sends `/report` command to the bot via Telegram.

**System Response:**
```
Please enter the period in format START-END (date format DD.MM.YYYY).
Example: 01.01.2024-31.12.2024
```

### Step 2: Provide Date Range
User sends date range in format: `DD.MM.YYYY-DD.MM.YYYY`

**Validations:**
- Format must match: `DD.MM.YYYY-DD.MM.YYYY`
- Start date must be before end date
- Date range must not exceed 1 year (365 days)
- Dates must be valid calendar dates

**System Response on Success:**
```
✅ Your report has been queued successfully!

The report will be generated and sent to you within a few minutes.
Please be patient.
```

**System Response on Error:**
```
Error: [specific error message]
```

### Step 3: Report Generation
The system processes the report task asynchronously:
1. Task is created with status `PENDING`
2. Scheduled processor picks up the task
3. Report is generated
4. File is sent to user via Telegram
5. Task is marked as `COMPLETED`

### Step 4: Receive Report
User receives the report file as a Telegram document with caption showing the date range.

## Retry Logic

### Configuration
```yaml
report:
  task:
    max-retries: 3                    # Maximum number of retry attempts
    retry-delay-minutes: 5            # Delay between retries (minutes)
    processor:
      delay-ms: 60000                 # Task processor interval (milliseconds)
```

### Retry Flow
1. Task starts with `retry_count = 0`
2. On failure, `retry_count` is incremented
3. If `retry_count < max_retries`:
   - Task status set to `PENDING`
   - `next_retry_at` set to current time + retry delay
   - Task will be picked up again by processor
4. If `retry_count >= max_retries`:
   - Task status set to `FAILED`
   - User receives failure notification via Telegram

### Failure Notification
When max retries are reached, user receives:
```
Sorry, we were unable to generate your report. Please try again later or contact support if the problem persists.
```

## Database Schema

### report_tasks
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Auto-generated task ID |
| telegram_id | BIGINT | Telegram user ID |
| chat_id | BIGINT | Telegram chat ID |
| start_date | DATE | Report period start |
| end_date | DATE | Report period end |
| status | VARCHAR(50) | PENDING, PROCESSING, COMPLETED, FAILED |
| retry_count | INT | Current retry attempt |
| max_retries | INT | Maximum allowed retries |
| error_message | TEXT | Error details (if failed) |
| created_at | TIMESTAMP | Task creation time |
| updated_at | TIMESTAMP | Last update time |
| next_retry_at | TIMESTAMP | Next scheduled retry time |

### telegram_user_states
| Column | Type | Description |
|--------|------|-------------|
| telegram_id | BIGINT (PK) | Telegram user ID |
| state | VARCHAR(50) | NONE, AWAITING_REPORT_DATES |
| updated_at | TIMESTAMP | Last state update |

## API Integration

### Telegram Bot API Endpoints Used

**sendMessage**
```
POST https://api.telegram.org/bot{token}/sendMessage
{
  "chat_id": 123456789,
  "text": "Message text"
}
```

**sendDocument**
```
POST https://api.telegram.org/bot{token}/sendDocument
Content-Type: multipart/form-data

{
  "chat_id": 123456789,
  "document": <file>,
  "caption": "Optional caption"
}
```

## Message Processing Rules

1. **Messages NOT saved to database:**
   - `/report` command
   - Date input messages
   - All other text messages (just logged and ignored)

2. **Messages from unlinked users:**
   - Ignored (user must link Telegram account first)

3. **Duplicate messages:**
   - Detected by `message_id`
   - Skipped if already processed

## Configuration

### Environment Variables
```bash
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=your-bot-token
TELEGRAM_WEBHOOK_SECRET=your-webhook-secret

# Report Task Configuration
REPORT_TASK_MAX_RETRIES=3
REPORT_TASK_RETRY_DELAY_MINUTES=5
REPORT_TASK_PROCESSOR_DELAY_MS=60000
```

### Application Properties
See `application.yml` for full configuration with defaults.

## Testing

### Manual Testing Steps

1. **Setup:**
   - Set `TELEGRAM_BOT_TOKEN` in `.env`
   - Start the application
   - Link your Telegram account via frontend

2. **Test /report command:**
   - Send `/report` to bot
   - Verify you receive instructions

3. **Test valid date input:**
   - Send: `01.01.2024-31.12.2024`
   - Verify success message
   - Wait ~1 minute
   - Verify you receive report file

4. **Test invalid formats:**
   - Wrong format: `01-01-2024 to 31-12-2024`
   - Start > End: `31.12.2024-01.01.2024`
   - Range > 1 year: `01.01.2023-31.12.2024`
   - Verify appropriate error messages

5. **Test retry logic:**
   - Manually set very short retry delay
   - Cause a failure (e.g., invalid bot token temporarily)
   - Verify retry attempts
   - Verify failure notification after max retries

## Future Enhancements

1. **Report Generation:**
   - Replace stub with actual financial data aggregation
   - Support multiple report formats (PDF, Excel, CSV)
   - Add charts and visualizations

2. **User Features:**
   - Cancel pending reports
   - View report history
   - Schedule recurring reports
   - Custom report templates

3. **Performance:**
   - Batch processing for multiple pending tasks
   - Caching for frequently requested reports
   - Database archival for old tasks

4. **Monitoring:**
   - Metrics for task success/failure rates
   - Alert on high failure rates
   - Dashboard for task queue status

## Troubleshooting

### Task stuck in PROCESSING
- Check logs for exceptions during report generation
- Manually reset task status to PENDING in database
- Increase retry delay if system is overloaded

### User not receiving reports
- Verify bot token is valid
- Check network connectivity to Telegram API
- Verify user's chat_id is correct in database
- Check bot has permission to send files

### High failure rate
- Check report generation service logs
- Verify database connectivity
- Check file system permissions for temp files
- Increase max retries or retry delay

## File Locations

### Source Files
```
src/main/java/ru/rgasymov/moneymanager/
├── domain/
│   └── entity/
│       ├── ReportTask.java
│       └── TelegramUserState.java
├── repository/
│   ├── ReportTaskRepository.java
│   └── TelegramUserStateRepository.java
├── service/
│   ├── telegram/
│   │   ├── TelegramService.java (updated)
│   │   └── TelegramBotClient.java
│   └── report/
│       ├── ReportGenerationService.java
│       └── ReportTaskProcessor.java

src/main/resources/
├── db/changelog/1.0/
│   └── report-tasks.yaml
└── application.yml (updated)
```

## References
- [Telegram Bot API Documentation](https://core.telegram.org/bots/api)
- [Spring Scheduling](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#scheduling)
