/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.core.parsing.antlr.extractor.statement.engine.ddl.dialect.mysql;

import io.shardingsphere.core.metadata.table.ColumnMetaData;
import io.shardingsphere.core.parsing.antlr.extractor.segment.constant.RuleName;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.DropPrimaryKeyExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.PrimaryKeyForAlterTableExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.RenameIndexExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.dialect.mysql.MySQLAddColumnExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.dialect.mysql.MySQLAddIndexExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.dialect.mysql.MySQLChangeColumnExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.dialect.mysql.MySQLDropIndexExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.segment.engine.dialect.mysql.MySQLModifyColumnExtractor;
import io.shardingsphere.core.parsing.antlr.extractor.statement.engine.ddl.AlterTableExtractor;
import io.shardingsphere.core.parsing.antlr.sql.segment.ColumnPositionSegment;
import io.shardingsphere.core.parsing.antlr.sql.statement.ddl.AlterTableStatement;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * MySQL alter table statement extractor.
 * 
 * @author duhongjun
 */
public final class MySQLAlterTableExtractor extends AlterTableExtractor {
    
    public MySQLAlterTableExtractor() {
        addSQLSegmentExtractor(new MySQLAddColumnExtractor());
        addSQLSegmentExtractor(new MySQLAddIndexExtractor());
        addSQLSegmentExtractor(new MySQLDropIndexExtractor());
        addSQLSegmentExtractor(new RenameIndexExtractor());
        addSQLSegmentExtractor(new PrimaryKeyForAlterTableExtractor(RuleName.ADD_CONSTRAINT));
        addSQLSegmentExtractor(new DropPrimaryKeyExtractor());
        addSQLSegmentExtractor(new MySQLChangeColumnExtractor());
        addSQLSegmentExtractor(new MySQLModifyColumnExtractor());
    }
    
    @Override
    protected void adjustColumnDefinition(final AlterTableStatement alterTableStatement, final List<ColumnMetaData> newColumnMetaData) {
        if (alterTableStatement.getPositionChangedColumns().isEmpty()) {
            return;
        }
        if (alterTableStatement.getPositionChangedColumns().size() > 1) {
            Collections.sort(alterTableStatement.getPositionChangedColumns());
        }
        for (ColumnPositionSegment each : alterTableStatement.getPositionChangedColumns()) {
            if (null != each.getFirstColumn()) {
                adjustFirst(newColumnMetaData, each.getFirstColumn());
            } else {
                adjustAfter(newColumnMetaData, each);
            }
        }
    }
    
    private void adjustFirst(final List<ColumnMetaData> newColumnMetaData, final String columnName) {
        ColumnMetaData firstMetaData = null;
        Iterator<ColumnMetaData> iterator = newColumnMetaData.iterator();
        while (iterator.hasNext()) {
            ColumnMetaData each = iterator.next();
            if (each.getColumnName().equals(columnName)) {
                firstMetaData = each;
                iterator.remove();
                break;
            }
        }
        if (null != firstMetaData) {
            newColumnMetaData.add(0, firstMetaData);
        }
    }
    
    private void adjustAfter(final List<ColumnMetaData> newColumnMetaData, final ColumnPositionSegment columnPosition) {
        int afterIndex = -1;
        int adjustColumnIndex = -1;
        for (int i = 0; i < newColumnMetaData.size(); i++) {
            if (newColumnMetaData.get(i).getColumnName().equals(columnPosition.getColumnName())) {
                adjustColumnIndex = i;
            }
            if (newColumnMetaData.get(i).getColumnName().equals(columnPosition.getAfterColumn())) {
                afterIndex = i;
            }
            if (adjustColumnIndex >= 0 && afterIndex >= 0) {
                break;
            }
        }
        if (adjustColumnIndex >= 0 && afterIndex >= 0 && adjustColumnIndex != afterIndex + 1) {
            ColumnMetaData adjustColumnMetaData = newColumnMetaData.remove(adjustColumnIndex);
            if (afterIndex < adjustColumnIndex) {
                afterIndex = afterIndex + 1;
            }
            newColumnMetaData.add(afterIndex, adjustColumnMetaData);
        }
    }
}
