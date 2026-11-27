<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="ru.etu.cinema.TicketCalculator,ru.etu.cinema.Html,java.math.BigDecimal" %>
<% request.setCharacterEncoding("UTF-8"); String error=null; BigDecimal total=null;
 if("POST".equals(request.getMethod())) {
  try { total=TicketCalculator.total(Integer.parseInt(request.getParameter("count")),"on".equals(request.getParameter("student"))); }
  catch(NumberFormatException e) { error="Введите целое количество билетов"; }
  catch(IllegalArgumentException e) { error=e.getMessage(); }
 } %>
<!doctype html><html lang="ru"><head><meta charset="UTF-8"><title>Билеты</title><link rel="stylesheet" href="style.css"></head><body>
<h1>Расчёт стоимости билетов</h1><p>Обычный билет — 400 руб. Скидка студенту — 20%.</p>
<form method="post"><label>Количество <input name="count" type="text" value="<%= Html.escape(request.getParameter("count")) %>"></label>
<label><input name="student" type="checkbox" <%= "on".equals(request.getParameter("student"))?"checked":"" %>> Студенческий билет</label><button>Рассчитать</button></form>
<% if(error!=null) { %><p class="error"><%= Html.escape(error) %></p><% } %>
<% if(total!=null) { %><p>Итого: <strong><%= total.toPlainString() %> руб.</strong></p><% } %>
</body></html>
