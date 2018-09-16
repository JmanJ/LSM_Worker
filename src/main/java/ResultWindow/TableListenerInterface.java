package ResultWindow;

/**
 * Created by JmanJ on 09.05.2016.
 */
public interface TableListenerInterface {

    void setCellVisible(Float index, Boolean flag);
    void selectCell(Float index);
    void deselectCell();
}
