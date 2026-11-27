package ru.etu.cinema.server;
import java.util.*;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import ru.etu.cinema.client.CatalogService;
import ru.etu.cinema.shared.FilmDto;
/** Серверная реализация афиши из лабораторной работы 3. */
public class CatalogServiceImpl extends RemoteServiceServlet implements CatalogService {
 private static final long serialVersionUID=1L;
 @Override public List<FilmDto> find(String genre) {
  if(!"all".equals(genre)&&!"drama".equals(genre)&&!"comedy".equals(genre)) throw new IllegalArgumentException("Неизвестный жанр");
  List<FilmDto> result=new ArrayList<>();
  FilmDto[] films={new FilmDto("Огни города","comedy","18:00",350),new FilmDto("Космический рейс","drama","19:30",450),new FilmDto("Дорога домой","drama","21:00",400)};
  for(FilmDto f:films) if("all".equals(genre)||genre.equals(f.getGenre())) result.add(f);
  return result;
 }
}
