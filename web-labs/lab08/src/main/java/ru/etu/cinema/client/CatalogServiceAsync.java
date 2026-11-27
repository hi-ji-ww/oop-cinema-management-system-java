package ru.etu.cinema.client;
import com.google.gwt.user.client.rpc.AsyncCallback;
import ru.etu.cinema.shared.FilmDto;
import java.util.List;
/** Асинхронный клиентский интерфейс RPC. */
public interface CatalogServiceAsync {
 void find(String genre,AsyncCallback<List<FilmDto>> callback);
}
