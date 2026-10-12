package com.zode.api.office.excel.util;

import jxl.write.WriteException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jxls.area.Area;
import org.jxls.command.AbstractCommand;
import org.jxls.command.Command;
import org.jxls.common.CellRef;
import org.jxls.common.Context;
import org.jxls.common.Size;
import org.jxls.transform.Transformer;
import org.jxls.transform.jexcel.JexcelTransformer;
import org.jxls.transform.poi.PoiCellData;
import org.jxls.transform.poi.PoiTransformer;

public class MergeCommand extends AbstractCommand {
    public static final String COMMAND_NAME = "merge";
    private String cols;    //合并的列数
    private String rows;    //合并的行数
    private Area area;
    private CellStyle cellStyle;    //第一个单元格的样式

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Command addArea(Area area) {
        if (super.getAreaList().size() >= 1) {
            throw new IllegalArgumentException("You can add only a single area to 'merge' command");
        }
        this.area = area;
        return super.addArea(area);
    }

    @Override
    public Size applyAt(CellRef cellRef, Context context) {
        int rows = 1, cols = 1;

        if (this.rows != null && this.rows.length() > 0) {
            Object rowsObj = getTransformationConfig().getExpressionEvaluator().evaluate(this.rows, context.toMap());
            if (rowsObj != null && isNumeric(rowsObj.toString())) {
                rows = toInt(rowsObj.toString());
            }
        }
        if (this.cols != null && this.cols.length() > 0) {
            Object colsObj = getTransformationConfig().getExpressionEvaluator().evaluate(this.cols, context.toMap());
            if (colsObj != null && isNumeric(colsObj.toString())) {
                cols = toInt(colsObj.toString());
            }
        }

        if (rows > 1 || cols > 1) {
            Transformer transformer = this.getTransformer();
            if (transformer instanceof PoiTransformer) {
                return poiMerge(cellRef, context, (PoiTransformer) transformer, rows, cols);
            } else if (transformer instanceof JexcelTransformer) {
                return jexcelMerge(cellRef, context, (JexcelTransformer) transformer, rows, cols);
            } else {
                throw new IllegalArgumentException("未找到相关类型");
            }
        }
        area.applyAt(cellRef, context);
        return new Size(1, 1);
    }

    private int toInt(String intStr) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private Size poiMerge(CellRef cellRef, Context context, PoiTransformer transformer, int rows, int cols) {
        Sheet sheet = transformer.getWorkbook().getSheet(cellRef.getSheetName());
        CellRangeAddress region = new CellRangeAddress(
                cellRef.getRow(),
                cellRef.getRow() + rows - 1,
                cellRef.getCol(),
                cellRef.getCol() + cols - 1);
        sheet.addMergedRegion(region);

        //合并之后单元格样式会丢失，以下操作将合并后的单元格恢复成合并前第一个单元格的样式
        area.applyAt(cellRef, context);
        if (cellStyle == null) {
            PoiCellData cellData = (PoiCellData) transformer.getCellData(cellRef);
            if (cellData != null) {
                cellStyle = cellData.getCellStyle();
            }
        }
        if (cellStyle != null) {
            setRegionStyle(cellStyle, region, sheet);
        }
        return new Size(cols, rows);
    }

    protected Size jexcelMerge(CellRef cellRef, Context context, JexcelTransformer transformer, int rows, int cols) {
        try {
            transformer.getWritableWorkbook().getSheet(cellRef.getSheetName())
                    .mergeCells(
                            cellRef.getRow(),
                            cellRef.getCol(),
                            cellRef.getRow() + rows - 1,
                            cellRef.getCol() + cols - 1);
            area.applyAt(cellRef, context);
        } catch (WriteException e) {
            throw new IllegalArgumentException("合并单元格失败");
        }
        return new Size(cols, rows);
    }

    private static void setRegionStyle(CellStyle cs, CellRangeAddress region, Sheet sheet) {
        for (int i = region.getFirstRow(); i <= region.getLastRow(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                row = sheet.createRow(i);
            for (int j = region.getFirstColumn(); j <= region.getLastColumn(); j++) {
                Cell cell = row.getCell(j);
                if (cell == null) {
                    cell = row.createCell(j);
                }
                cell.setCellStyle(cs);
            }
        }
    }

    public static boolean isNumeric(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        } else {
            int sz = cs.length();

            for (int i = 0; i < sz; ++i) {
                if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }

    public String getCols() {
        return cols;
    }

    public void setCols(String cols) {
        this.cols = cols;
    }

    public String getRows() {
        return rows;
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

}
