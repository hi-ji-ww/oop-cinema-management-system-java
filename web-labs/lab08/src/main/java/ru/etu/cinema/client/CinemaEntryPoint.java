package ru.etu.cinema.client;
import java.util.List;
import com.google.gwt.core.client.*;
import com.google.gwt.user.client.rpc.*;
import com.google.gwt.user.client.ui.*;
import ru.etu.cinema.shared.FilmDto;
/** Клиентский интерфейс афиши, компилируемый GWT в JavaScript. */
public class CinemaEntryPoint implements EntryPoint {
 private final CatalogServiceAsync service=GWT.create(CatalogService.class);
 @Override public void onModuleLoad() {
  VerticalPanel panel=new VerticalPanel(); panel.setSpacing(12);
  panel.add(new Label("Афиша кинотеатра Север"));
  ListBox genre=new ListBox(); genre.addItem("Все жанры","all");genre.addItem("Драма","drama");genre.addItem("Комедия","comedy");
  Button show=new Button("Показать"); Label status=new Label(); FlexTable table=new FlexTable(); table.setBorderWidth(1);table.setCellPadding(12);
  panel.add(genre);panel.add(show);panel.add(status);panel.add(table);RootPanel.get("app").add(panel);
  show.addClickHandler(event->{ show.setEnabled(false);status.setText("Загрузка...");
   service.find(genre.getSelectedValue(),new AsyncCallback<List<FilmDto>>() {
    public void onFailure(Throwable error){show.setEnabled(true);status.setText("Ошибка получения афиши");}
    public void onSuccess(List<FilmDto> films){
     show.setEnabled(true);table.removeAllRows();
     String[] headers={"Фильм","Жанр","Начало","Цена, руб."};for(int j=0;j<4;j++)table.setText(0,j,headers[j]);
     int row=1;for(FilmDto f:films){table.setText(row,0,f.getTitle());table.setText(row,1,"drama".equals(f.getGenre())?"Драма":"Комедия");table.setText(row,2,f.getTime());table.setText(row,3,String.valueOf(f.getPrice()));row++;}
     status.setText("Найдено сеансов: "+films.size());
    }
   });
  }); show.click();
 }
}
