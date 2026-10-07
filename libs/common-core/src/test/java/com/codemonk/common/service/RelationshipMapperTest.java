package com.codemonk.common.service;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.stereotype.Component;



public class RelationshipMapperTest {

    private RelationshipMapper mapper;

    @BeforeEach
    void setUp(){
        mapper = new RelationshipMapper();
    }

    @Test
    @DisplayName("toResponse should map all fields from RelationshipEntity to RelationshipResponse")
    public void toResponse_shouldMapAllFields(){
        RelationshipEntity entity = new RelationshipEntity("n-1","n-2","CALLS");
        entity.setId(42L);

        RelationshipResponse response = mapper.toResponse(entity);
        assertEquals("42",response.id());
        assertEquals("n-1",response.sourceNodeId());
        assertEquals("n-2",response.targetNodeId());
        assertEquals("CALLS",response.type());
    }

    @Test
    @DisplayName("toResponse should map null id to empty string")
    public void toResponse_shouldMapNullIdToEmptyString(){
        RelationshipEntity entity = new RelationshipEntity("n-1","n-2","CALLS");

        RelationshipResponse response = mapper.toResponse(entity);
        assertEquals("",response.id());
    }


    @Test
    @DisplayName("toDomain should map all fields from RelationshipResponse to RelationshipEntity")
    public void toDomain_shouldMapAllFields(){
        RelationshipResponse response = new RelationshipResponse("7","n-1","n-2","IMPORTS");

        RelationshipEntity entity = mapper.toDomain(response);

        assertEquals(7L,entity.getId());
        assertEquals("n-1",entity.getSourceNodeId());
        assertEquals("n-2",entity.getTargetNodeId());
        assertEquals("IMPORTS",entity.getType());
    }

    @Test
    @DisplayName("toDomain should leave id unset when response id is blank")
    public void toDomain_shouldLeaveIdUnsetWhenBlank(){

        RelationshipResponse response = new RelationshipResponse("","n-1","n-2","IMPORTS");
        RelationshipEntity entity = mapper.toDomain(response);

        assertNull(entity.getId());
    }

    @Test
    @DisplayName("toResponse then toDomain round-trip should preserve all values")
    public void roundTrip_toResponseThenToDomain(){
        RelationshipEntity original = new RelationshipEntity("n-3","n-4","CONTAINS");
        original.setId(99L);

        RelationshipEntity result = mapper.toDomain(mapper.toResponse(original));

        assertEquals(original.getId(),result.getId());
        assertEquals(original.getSourceNodeId(),result.getSourceNodeId());
        assertEquals(original.getTargetNodeId(),result.getTargetNodeId());


    }

    @Test
    @DisplayName("toDomain then toResponse round-trip should preserve all values")
    public void roundTrip_toDomainThenToResponse() {
        RelationshipResponse original = new RelationshipResponse("5", "n-1", "n-2", "INHERITS");

        RelationshipResponse result = mapper.toResponse(mapper.toDomain(original));

        assertEquals(original, result);
    }

    @Test
    @DisplayName("toResponse should throw NullPointerException for null input")
    public void toResponse_shouldThrowOnNull() {

        assertThrows(NullPointerException.class, () -> mapper.toResponse(null));
    }

    @Test
    @DisplayName("toDomain should throw NullPointerException for null input")
    public void toDomain_shouldThrowOnNull() {

        assertThrows(NullPointerException.class, () -> mapper.toDomain(null));
    }

    @Test
    @DisplayName("RelationshipMapper should be annotated with @Component")
    public void shouldBeAnnotatedWithComponent() {

        assertTrue(RelationshipMapper.class.isAnnotationPresent(Component.class));
    }

}
