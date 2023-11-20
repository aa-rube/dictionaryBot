package app.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfig {
    @Value("${bot.username}")
    String botName;

    @Value("${bot.token}")
    String token;

    @Value("${admin.chat.id}")
    long adminChatId;

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getAdminChatId() {
        return adminChatId;
    }

    public void setAdminChatId(long adminChatId) {
        this.adminChatId = adminChatId;
    }
}