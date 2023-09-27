package dass_21;

//Manipular archivos Office
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//Realizar gráficos 
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


//Libreria para manipular maquinas de soporte vectorial
import libsvm.*;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_problem;
import libsvm.svm_node;
import libsvm.svm_parameter;


public class dass_21 {

    public static void main(String[] args) {
        try {
            // Cargar el archivo de Excel
            FileInputStream file = new FileInputStream(new File("D:\\Este equipo\\Escritorio\\encuesta.xlsx"));

            // Crear el libro de trabajo de Excel
            Workbook workbook = new XSSFWorkbook(file);

            // Obtener la primera hoja del libro de trabajo
            Sheet sheet = workbook.getSheetAt(0);

            // Crear el conjunto de datos para el diagrama de barras
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            // Crear un formateador para obtener el valor de la celda como una cadena
            DataFormatter dataFormatter = new DataFormatter();

            // Iterar sobre las filas y columnas del archivo de Excel
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.iterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    if (cell.getCellType() == CellType.NUMERIC) {
                        double numericValue = cell.getNumericCellValue();
                        String category = dataFormatter.formatCellValue(cell);
                        dataset.addValue(numericValue, "Datos", category);
                    }
                }
            }

            // Cerrar el archivo de Excel
            file.close();

            // Entrenar el modelo SVM
            svm_model model = trainSVMModel(dataset);

            // Realizar predicciones con el modelo SVM
            predictSVMLabels(dataset, model);

            // Crear el diagrama de barras
            JFreeChart barChart = ChartFactory.createBarChart(
                    "Resultados - Encuesta DASS-21", // Título del gráfico
                    "Enfermedad", // Etiqueta del eje X
                    "Porcentaje", // Etiqueta del eje Y
                    dataset, // Conjunto de datos
                    PlotOrientation.VERTICAL, // Orientación del gráfico
                    true, // Mostrar leyenda
                    true, // Mostrar tooltips
                    false // Mostrar URLs
            );

            // Guardar el diagrama de barras como una imagen
            int width = 1280; // Ancho de la imagen en píxeles
            int height = 720; // Altura de la imagen en píxeles
            ChartUtilities.saveChartAsPNG(new File("D:\\Este equipo\\Escritorio\\salida.png"), barChart, width, height);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static svm_model trainSVMModel(DefaultCategoryDataset dataset) {
        // Convertir los datos del conjunto de datos en un formato adecuado para SVM
        svm_problem problem = convertDatasetToSVMProblem(dataset);

        // Configurar los parámetros del modelo SVM
        svm_parameter parameters = new svm_parameter();
        parameters.svm_type = svm_parameter.C_SVC;
        parameters.kernel_type = svm_parameter.RBF;
        parameters.C = 1;

        // Entrenar el modelo SVM
        return svm.svm_train(problem, parameters);
    }

    private static svm_problem convertDatasetToSVMProblem(DefaultCategoryDataset dataset) {
        // Obtener los datos y etiquetas del conjunto de datos
        double[][] data = extractDataFromDataset(dataset);
        int[] labels = extractLabelsFromDataset(dataset);

        // Crear un problema SVM
        svm_problem problem = new svm_problem();
        problem.l = data.length;
        problem.x = new svm_node[data.length][];
        problem.y = new double[data.length];

        // Rellenar el problema SVM con los datos y etiquetas
        for (int i = 0; i < data.length; i++) {
            problem.x[i] = createSVMNodes(data[i]);
            problem.y[i] = labels[i];
        }

        return problem;
    }

    private static double[][] extractDataFromDataset(DefaultCategoryDataset dataset) {
        List<double[]> dataList = new ArrayList<>();
        for (int i = 0; i < dataset.getRowCount(); i++) {
            double[] rowData = new double[dataset.getColumnCount()];
            for (int j = 0; j < dataset.getColumnCount(); j++) {
                rowData[j] = dataset.getValue(i, j).doubleValue();
            }
            dataList.add(rowData);
        }

        double[][] data = new double[dataList.size()][];
        for (int i = 0; i < dataList.size(); i++) {
            data[i] = dataList.get(i);
        }

        return data;
    }

    private static int[] extractLabelsFromDataset(DefaultCategoryDataset dataset) {
        int[] labels = new int[dataset.getRowCount()];
        for (int i = 0; i < dataset.getRowCount(); i++) {
            labels[i] = i + 1; // Asignar etiquetas consecutivas a cada fila
        }
        return labels;
    }

    private static svm_node[] createSVMNodes(double[] data) {
        svm_node[] nodes = new svm_node[data.length];
        for (int i = 0; i < data.length; i++) {
            svm_node node = new svm_node();
            node.index = i + 1; // Los índices de las características en SVM comienzan en 1
            node.value = data[i];
            nodes[i] = node;
        }
        return nodes;
    }
    
    private static void predictSVMLabels(DefaultCategoryDataset dataset, svm_model model) {
        // Convertir los datos del conjunto de datos en un formato adecuado para SVM
        svm_problem problem = convertDatasetToSVMProblem(dataset);
        
        // Crear un array de svm_node[][] para svm_predict_values
        svm_node[][] svmNodes = problem.x;

        // Realizar predicciones con el modelo SVM
        double[] predictedLabels = new double[problem.l];
        svm.svm_predict_values(model, svmNodes, predictedLabels);

        // Imprimir las etiquetas predichas o guardarlas para su posterior uso
        for (double label : predictedLabels) {
            System.out.println("Etiqueta predicha: " + label);
        }
    }
}
