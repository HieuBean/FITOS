package com.fitos.controller;

import com.fitos.model.Studio;
import com.fitos.repository.StudioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/studios")
@CrossOrigin(origins = "*")
public class StudioController {

    @Autowired
    private StudioRepository studioRepository;

    @GetMapping
    public List<Studio> getAllStudios() {
        return studioRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Studio createStudio(@RequestBody Studio studio) {
        return studioRepository.save(studio);
    }
}
