// 
// MobeelizerModelImpl.java
// 
// Copyright (C) 2012 Mobeelizer Ltd. All Rights Reserved.
//
// Mobeelizer SDK is free software; you can redistribute it and/or modify it 
// under the terms of the GNU Affero General Public License as published by 
// the Free Software Foundation; either version 3 of the License, or (at your
// option) any later version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
// FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
// for more details.
//
// You should have received a copy of the GNU Affero General Public License 
// along with this program; if not, write to the Free Software Foundation, Inc., 
// 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
// 

package com.mobeelizer.java.model;

import static com.mobeelizer.java.model.MobeelizerReflectionUtil.getField;
import static com.mobeelizer.java.model.MobeelizerReflectionUtil.getOptionalField;
import static com.mobeelizer.java.model.MobeelizerReflectionUtil.getValue;
import static com.mobeelizer.java.model.MobeelizerReflectionUtil.setValue;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mobeelizer.java.api.MobeelizerErrorsBuilder;
import com.mobeelizer.java.api.MobeelizerField;
import com.mobeelizer.java.api.MobeelizerModel;
import com.mobeelizer.java.api.MobeelizerModelCredentials;
import com.mobeelizer.java.definition.MobeelizerModelCredentialsDefinition;
import com.mobeelizer.java.sync.MobeelizerJsonEntity;
import com.mobeelizer.java.sync.MobeelizerJsonEntity.ConflictState;

public class MobeelizerModelImpl implements MobeelizerModel {

    private final Class<?> clazz;

    private final MobeelizerFieldAccessor guidField;

    private final MobeelizerFieldAccessor ownerField;

    private final MobeelizerFieldAccessor groupField;

    private final MobeelizerFieldAccessor conflictedField;

    private final MobeelizerFieldAccessor modifiedField;

    private final MobeelizerFieldAccessor deletedField;
    
    private final MobeelizerFieldAccessor resolveConflictField;

    private final Set<MobeelizerField> fields;

    private final String name;

    private final MobeelizerModelCredentials credentials;

    public MobeelizerModelImpl(final Class<?> clazz, final String name, final MobeelizerModelCredentialsDefinition credentials,
            final Set<MobeelizerField> fields) {
        this.clazz = clazz;
        this.name = name;
        this.fields = fields;
        this.credentials = new MobeelizerModelCredentialsImpl(credentials);
        if (clazz != null) {
            guidField = new ReflectionMobeelizerFieldAccessor(getField(clazz, "guid", String.class));
            ownerField = getOptionalFieldAccessor(clazz, "owner", String.class);
            groupField = getOptionalFieldAccessor(clazz, "group", String.class);
            conflictedField = getOptionalFieldAccessor(clazz, "conflicted", Boolean.TYPE);
            modifiedField = getOptionalFieldAccessor(clazz, "modified", Boolean.TYPE);
            deletedField = getOptionalFieldAccessor(clazz, "deleted", Boolean.TYPE);
            resolveConflictField = getOptionalFieldAccessor(clazz, "resolveConflict", Boolean.TYPE);
        } else {
            guidField = new BasicMobeelizerFieldAccessor("guid", String.class);
            ownerField = new BasicMobeelizerFieldAccessor("owner", String.class);
            groupField = new BasicMobeelizerFieldAccessor("group", String.class);
            conflictedField = new BasicMobeelizerFieldAccessor("conflicted", Boolean.class);
            modifiedField = new BasicMobeelizerFieldAccessor("modified", Boolean.class);
            deletedField = new BasicMobeelizerFieldAccessor("deleted", Boolean.class);
            resolveConflictField = new BasicMobeelizerFieldAccessor("resolveConflict", Boolean.class);
        }
    }

    private MobeelizerFieldAccessor getOptionalFieldAccessor(final Class<?> clazz, final String name, final Class<?> type) {
        Field fieldAccesor = getOptionalField(clazz, name, type);
        if (fieldAccesor == null) {
            return null;
        } else {
            return new ReflectionMobeelizerFieldAccessor(fieldAccesor);
        }
    }

    @Override
    public Class<?> getMappingClass() {
        return clazz;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MobeelizerModelCredentials getCredentials() {
        return credentials;
    }

    @Override
    public Set<MobeelizerField> getFields() {
        return new HashSet<MobeelizerField>(fields);
    }

    public MobeelizerFieldAccessor getGuidField() {
        return guidField;
    }

    public MobeelizerFieldAccessor getOwnerField() {
        return ownerField;
    }

    public MobeelizerFieldAccessor getGroupField() {
        return groupField;
    }

    public MobeelizerFieldAccessor getModifiedField() {
        return modifiedField;
    }

    public MobeelizerFieldAccessor getDeletedField() {
        return deletedField;
    }

    public MobeelizerFieldAccessor getConflictedField() {
        return conflictedField;
    }

    public Object getEntityFromJsonEntity(final MobeelizerJsonEntity json) {
        try {
            Object entity = clazz.newInstance();

            setValue(guidField, entity, json.getGuid());

            if (ownerField != null) {
                setValue(ownerField, entity, json.getOwner());
            }

            if (groupField != null) {
                setValue(groupField, entity, json.getGroup());
            }

            if (conflictedField != null) {
                setValue(conflictedField, entity,
                        json.getConflictState() != null && !json.getConflictState().equals(ConflictState.NO_IN_CONFLICT));
            }


            if (json.getFields() != null) {
				if (deletedField != null) {
					setValue(deletedField, entity, json.isDeleted());
				}
                for (MobeelizerField field : this.fields) {
                    ((MobeelizerFieldImpl) field).setValueFromJsonEntityToEntity(json.getFields(), entity);
                }
            }

            return entity;
        } catch (InstantiationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public MobeelizerJsonEntity getJsonEntityFromEntity(final Object entity, final MobeelizerErrorsBuilder errors) {
        MobeelizerJsonEntity json = new MobeelizerJsonEntity();
        json.setModel(name);
        json.setGuid((String) getValue(guidField, entity));

        if (ownerField != null) {
            json.setOwner((String) getValue(ownerField, entity));
        }

        if (groupField != null) {
            json.setGroup((String) getValue(groupField, entity));
        }

        if (resolveConflictField != null){
        	if((Boolean) getValue(resolveConflictField, entity)){
        		json.setResolveConflict((Boolean) getValue(resolveConflictField, entity));
        	}
        }
        
        if (conflictedField != null) {
            json.setConflictState((Boolean) getValue(conflictedField, entity) ? ConflictState.IN_CONFLICT
                    : ConflictState.NO_IN_CONFLICT);
        } else {
            json.setConflictState(ConflictState.NO_IN_CONFLICT);
        }

        Map<String, String> values = new HashMap<String, String>();

        if (deletedField != null) {
            values.put("s_deleted", (Boolean) getValue(deletedField, entity) ? "true" : "false");
        } else {
            values.put("s_deleted", "false");
        }

        for (MobeelizerField field : fields) {
            ((MobeelizerFieldImpl) field).setValueFromEntityToJsonEntity(entity, values, errors);
        }

        json.setFields(values);

        return json;
    }

}
