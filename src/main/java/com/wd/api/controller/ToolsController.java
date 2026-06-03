package com.wd.api.controller;

import com.wd.api.model.SqftCategories;
import com.wd.api.dao.interfaces.IToolsDAO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tools")
public class ToolsController {

    private final IToolsDAO toolsDAO;

    public ToolsController(IToolsDAO toolsDAO) {
        this.toolsDAO = toolsDAO;
    }

    @GetMapping("/getwdsqftcategories")
    public List<SqftCategories> getWdSqftCategories() {
        return toolsDAO.getAllSqftCategories();
    }

}