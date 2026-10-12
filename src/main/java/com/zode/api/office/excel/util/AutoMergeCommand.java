package com.zode.api.office.excel.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
import org.jxls.transform.poi.PoiTransformer;

import java.util.List;

/**
 * 此指令用于自动合并单元格,只支持单个方向合并，默认列合并
 * jx:autoMerge(mergeStrategy="row" length="XXX.size()" lastCell="E3")
 * mergeStrategy="row"  纵向合并可不填
 */
public class AutoMergeCommand extends AbstractCommand {

    public static final String COMMAND_NAME = "autoMerge";

    public static final String VERTICAL_MERGE = "row";

    public static final String HORIZONTAL_MERGE = "col";

    private Area area;

    private int[] rows;    // 记录行

    private int cols;    // 记录列

    private int nowRowNum = 0;    // 记录列

    private String mergeStrategy = VERTICAL_MERGE; // 合并策略 row: 纵向 col: 横向

    private String length; // 遍历区的总长度

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Command addArea(Area area) {
        if (super.getAreaList().size() >= 1) {
            throw new IllegalArgumentException("You can add only a single area to 'autoMerge' command");
        }
        this.area = area;
        this.rows = new int[area.getSize().getWidth()];
        return super.addArea(area);
    }

    @Override
    public List<Area> getAreaList() {
        return super.getAreaList();
    }

    @Override
    public Size applyAt(CellRef cellRef, Context context) {
        area.applyAt(cellRef, context);
        int length = 0;
        Transformer transformer = area.getTransformer();
        if (transformer instanceof PoiTransformer) {
            if (StrUtil.isNotBlank(this.length)) {
                Object lengthObj = getTransformationConfig().getExpressionEvaluator().evaluate(this.length, context.toMap());
                if (ObjectUtil.isNotEmpty(lengthObj)) {
                    length = Integer.parseInt(lengthObj.toString());
                }
            }
            return poiAutoMerge(cellRef, context, (PoiTransformer) transformer, this.mergeStrategy, length);
        } else if (transformer instanceof JexcelTransformer) {
            return null;
        } else {
            throw new IllegalArgumentException("未找到相关类型");
        }
    }

    private Size poiAutoMerge(CellRef cellRef, Context context, PoiTransformer transformer, String mergeStrategy, int length) {
        Sheet sheet = transformer.getWorkbook().getSheet(cellRef.getSheetName());
        this.nowRowNum += 1;
        for (int i = 0; i < area.getSize().getWidth(); i++) {
            int currentRow = cellRef.getRow();
            int currentCol = cellRef.getCol() + i;
            int startMergeRow = 0, endMergeRow = 0, startMergeCol = 0, endMergeCol = 0;
            String cellValue = sheet.getRow(currentRow).getCell(currentCol).getStringCellValue();
            if (VERTICAL_MERGE.equals(mergeStrategy)) {
                String topCellValue = sheet.getRow(currentRow - 1).getCell(currentCol).getStringCellValue();
                if ((StrUtil.isNotBlank(cellValue) && StrUtil.equals(cellValue, topCellValue))) {
                    this.rows[i] += 1;
                } else {
                    if (this.rows[i] > 0) {
                        startMergeRow = currentRow - rows[i] - 1;
                        endMergeRow = currentRow - 1;
                        CellRangeAddress region = new CellRangeAddress(startMergeRow, endMergeRow, currentCol, currentCol);
                        sheet.addMergedRegion(region);
                        this.rows[i] = 0;
                    }
                }
                if (this.rows[i] > 0 && this.nowRowNum == length) {
                    startMergeRow = currentRow - rows[i];
                    endMergeRow = currentRow;
                    CellRangeAddress region = new CellRangeAddress(startMergeRow, endMergeRow, currentCol, currentCol);
                    sheet.addMergedRegion(region);
                    this.rows[i] = 0;
                }
            }
            if (HORIZONTAL_MERGE.equals(mergeStrategy) && currentCol > area.getStartCellRef().getCol()) {
                String leftCellValue = sheet.getRow(currentRow).getCell(currentCol - 1).getStringCellValue();
                if (StrUtil.isNotBlank(cellValue) && StrUtil.equals(cellValue, leftCellValue)) {
                    this.cols += 1;
                } else {
                    if (this.cols > 0) {
                        startMergeCol = currentCol - this.cols - 1;
                        endMergeCol = currentCol - 1;
                        CellRangeAddress region = new CellRangeAddress(currentRow, currentRow, startMergeCol, endMergeCol);
                        sheet.addMergedRegion(region);
                        this.cols = 0;
                    }
                }
                if (this.cols > 0 && i == area.getSize().getWidth() - 1) {
                    startMergeCol = currentCol - this.cols;
                    endMergeCol = currentCol;
                    CellRangeAddress region = new CellRangeAddress(currentRow, currentRow, startMergeCol, endMergeCol);
                    sheet.addMergedRegion(region);
                    this.cols = 0;
                }
            }
        }
        return area.getSize();
    }

    public String getMergeStrategy() {
        return mergeStrategy;
    }

    public void setMergeStrategy(String mergeStrategy) {
        this.mergeStrategy = mergeStrategy;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }
}
