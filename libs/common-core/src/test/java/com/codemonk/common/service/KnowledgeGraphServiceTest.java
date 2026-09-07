package com.codemonk.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class KnowledgeGraphServiceTest {

    @Test
    @DisplayName("KnowledgeGraphService should be an interface")
    void shouldBeAnInterface() {
        assertTrue(KnowledgeGraphService.class.isInterface());
    }

    @Test
    @DisplayName("Should define createNode method with correct signature")
    void shouldDefineCreateNode() throws NoSuchMethodException {
        Method method = KnowledgeGraphService.class.getMethod("createNode", String.class, String.class);
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("Should define createRelationship method with correct signature")
    void shouldDefineCreateRelationship() throws NoSuchMethodException {
        Method method = KnowledgeGraphService.class.getMethod("createRelationship", String.class, String.class,
                String.class);
        assertEquals(String.class, method.getReturnType());
    }

    @Test
    @DisplayName("Should define findById method returning Optional")
    void shouldDefineFindById() throws NoSuchMethodException {
        Method method = KnowledgeGraphService.class.getMethod("findById", String.class);
        assertEquals(Optional.class, method.getReturnType());
    }

    @Test
    @DisplayName("Should define findNeighbors method returning GET.Neighborhood")
    void shouldDefineFindNeighbors() throws NoSuchMethodException {
        Method method = KnowledgeGraphService.class.getMethod("findNeighbors", String.class);
        assertEquals(GET.Neighborhood.class, method.getReturnType());
    }
}
