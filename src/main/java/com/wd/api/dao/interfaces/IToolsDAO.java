package com.wd.api.dao.interfaces;

import com.wd.api.model.Lead;
import com.wd.api.model.SqftCategories;
import java.util.List;

public interface IToolsDAO {

    List<SqftCategories> getAllSqftCategories();

    int saveLeadEstimate(Lead lead);
}
