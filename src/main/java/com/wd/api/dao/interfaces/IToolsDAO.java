package com.wd.api.dao.interfaces;

import com.wd.api.dao.model.Leads;
import com.wd.api.dao.model.SqftCategories;
import java.util.List;

public interface IToolsDAO {

    List<SqftCategories> getAllSqftCategories();

    int saveLeadEstimate(Leads lead);
}
