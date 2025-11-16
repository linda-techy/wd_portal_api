package com.wd.api.controller;

import com.wd.api.dao.model.SqftCategories;
import com.wd.api.dao.interfaces.IToolsDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tools")
public class ToolsController {

    @Autowired
    private IToolsDAO toolsDAO;

    @GetMapping("/getwdsqftcategories")
    public List<SqftCategories> getWdSqftCategories() {
        return toolsDAO.getAllSqftCategories();
    }

}