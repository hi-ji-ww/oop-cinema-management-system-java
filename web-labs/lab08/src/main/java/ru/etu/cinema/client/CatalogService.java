package ru.etu.cinema.client;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import ru.etu.cinema.shared.FilmDto;
import java.util.List;
/** Контракт чтения афиши через RPC. */
@RemoteServiceRelativePath("catalog")
public interface CatalogService extends RemoteService {
 /** @param genre all, drama или comedy
  * @return отфильтрованные сеансы
  * @throws IllegalArgumentException если жанр не поддерживается */
 List<FilmDto> find(String genre) throws IllegalArgumentException;
}
