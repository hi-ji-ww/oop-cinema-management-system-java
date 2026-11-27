<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.net.URLDecoder,ru.etu.cinema.Html" %>
<% String name="Гость", color="ffffff";
 // Cookie читаются из нового HTTP запроса, а не из response.
 if(request.getCookies()!=null) for(Cookie c:request.getCookies()) {
  try {
   if("cinemaName".equals(c.getName())) name=URLDecoder.decode(c.getValue(),"UTF-8");
   if("cinemaColor".equals(c.getName())&&c.getValue().matches("[0-9A-Fa-f]{6}")) color=c.getValue();
  } catch(IllegalArgumentException ignored) { name="Гость"; }
 }
 Integer visits; String last,previous;
 synchronized(session) { visits=(Integer)session.getAttribute("visits"); last=(String)session.getAttribute("lastVisit"); previous=(String)session.getAttribute("previousVisit"); }
%>
<!doctype html><html lang="ru"><meta charset="UTF-8"><title>Данные сеанса</title><link rel="stylesheet" href="style.css"><body style="background-color:#<%= color %>">
<h1>Данные посетителя</h1><p>Имя из Cookie: <%= Html.escape(name) %></p><p>Цвет из Cookie: #<%= color %></p>
<p>Количество отправок формы в сессии: <%= visits==null?0:visits %></p>
<p>Последнее обращение: <%= last==null?"Нет":Html.escape(last) %></p><p>Предыдущее обращение: <%= previous==null?"Нет":Html.escape(previous) %></p>
<a href="index.jsp">Изменить настройки</a></body></html>
