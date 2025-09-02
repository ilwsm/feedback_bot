package org.example.admin;

public final class HTML {
    final public static String[] FEEDBACKS = {"""
    <html lang="">
      <head><title>Feedbacks</title></head>
      <body style="font-family:sans-serif; margin:40px;">
        <h2>Список відгуків</h2>
        
        <form method="get" action="/feedbacks">
          <label>Філія:
            <select name="branch">
              <option value="">-- Всі --</option>
              %s
            </select>
          </label>
          
          <label>Посада:
            <select name="role">
              <option value="">-- Всі --</option>
              %s
            </select>
          </label>

          <label>Критичність:
            <select name="criticality">
              <option value="">-- Всі --</option>
              %s
            </select>
          </label>
          
          <button type="submit">Фільтрувати</button>
        </form>
        
        <form method="get" action="/feedbacks/export" style="display: inline;">
          <input type="hidden" name="branch" value="%4$s">
          <input type="hidden" name="role" value="%5$s">
          <input type="hidden" name="criticality" value="%6$s">
          <button type="submit" style="background-color: #4CAF50; color: white; padding: 5px 10px; border: none; cursor: pointer;">
            Експорт в CSV
          </button>
        </form>
        
        <br>
        
        <table border="1" cellpadding="5" cellspacing="0">
          <tr>
            <th>ID</th><th>Message</th><th>Sentiment</th><th>Criticality</th>
            <th>Recommendation</th><th>ChatId</th><th>User Role</th><th>User Branch</th><th>Created At</th>
          </tr>
    """, """
    </table></body></html>
    """
    };


    final public static String WELCOME = """
            <html lang="">
              <head><title>Feedback Admin</title></head>
              <body style="font-family:sans-serif; margin:40px;">
                <h2>Адмін-панель Feedback Bot</h2>
                <p>Ця адмінка дозволяє переглядати відгуки користувачів з БД.</p>
                <p>Можна буде додати фільтрацію по філії, посаді і рівню критичності.</p>
                <button onclick="location.href='/feedbacks'">Переглянути відгуки</button>
              </body>
            </html>
            """;


}
