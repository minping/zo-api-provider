package com.zode.api.office.excel.util;

import org.apache.poi.ss.usermodel.*;
import org.jxls.area.Area;
import org.jxls.command.AbstractCommand;
import org.jxls.command.Command;
import org.jxls.common.CellRef;
import org.jxls.common.Context;
import org.jxls.common.Size;
import org.jxls.transform.poi.PoiTransformer;

/**
 * 这个方法用于字体
 */
public class FontCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "font";
    private Area area;
    /**
     * HSSFColor
     */
    private String color; // 颜色

    private String bold; // 是否设置颜色

    private CellStyle cellStyle;    //第一个单元格的样式

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Command addArea(Area area) {
        if (super.getAreaList().size() >= 1) {
            throw new IllegalArgumentException("You can add only a single area to 'font' command");
        }
        this.area = area;
        return super.addArea(area);
    }

    @Override
    public Size applyAt(CellRef cellRef, Context context) {
        area.applyAt(cellRef, context);
        short color = 0;
        Boolean bold = false;
        Object colorEva = getTransformationConfig().getExpressionEvaluator().evaluate(this.color, context.toMap());
        if(colorEva != null &&  colorEva instanceof Short) {
            color = (short) colorEva;
        }
        Object boldEva = getTransformationConfig().getExpressionEvaluator().evaluate(this.bold, context.toMap());
        if(boldEva != null && boldEva instanceof Boolean) {
            bold = (Boolean) boldEva;
        }
        PoiTransformer transformer = (PoiTransformer) this.getTransformer();
        Workbook workbook = transformer.getWorkbook();
        Sheet sheet = workbook.getSheet(cellRef.getSheetName());
        Cell cell = sheet.getRow(cellRef.getRow()).getCell(cellRef.getCol());
        cellStyle = cell.getCellStyle();
        Font font = workbook.createFont();
        CellStyle newStyle = workbook.createCellStyle();
        if (bold) {
            font.setBold(true);
        }
        if (color != 0) {
            font.setColor(color);
        }
        newStyle.cloneStyleFrom(cellStyle);
        newStyle.setFont(font);
        cell.setCellStyle(newStyle);
        return area.getSize();
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBold() {
        return bold;
    }

    public void setBold(String bold) {
        this.bold = bold;
    }
}
