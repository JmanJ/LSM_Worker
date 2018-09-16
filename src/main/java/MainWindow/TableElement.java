package MainWindow;


public class TableElement {

    private int cellIndex;
    private Double cellSize;
    private int coreIndex;
    private Double coreSize;
    private Double probability;

    TableElement(int cellIndex, Double cellSize, int coreIndex, Double coreSize, Double probability){
        this.cellIndex = cellIndex;
        this.cellSize = cellSize;
        this.coreIndex = coreIndex;
        this.coreSize = coreSize;
        this.probability = probability;
    }
    public int getCellIndex() {
        return cellIndex;
    }

    public Double getCellSize() {
        return cellSize;
    }

    public int getCoreIndex() {
        return coreIndex;
    }

    public Double getCoreSize() {
        return coreSize;
    }

    public Double getProbability() { return probability; }
}
