package ru.etu.cinema.shared;
import java.io.Serializable;
/** Сериализуемая запись сеанса для передачи через GWT RPC. */
public class FilmDto implements Serializable {
 private static final long serialVersionUID=1L;
 private String title,genre,time; private int price;
 public FilmDto() {}
 public FilmDto(String title,String genre,String time,int price) { this.title=title;this.genre=genre;this.time=time;this.price=price; }
 public String getTitle(){return title;} public String getGenre(){return genre;}
 public String getTime(){return time;} public int getPrice(){return price;}
}
