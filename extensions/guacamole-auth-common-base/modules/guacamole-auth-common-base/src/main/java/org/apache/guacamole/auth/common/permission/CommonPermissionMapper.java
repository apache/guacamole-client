package org.apache.guacamole.auth.common.permission;

public interface CommonPermissionMapper<PermissionModelType extends ObjectPermissionModelInterface>
	extends ObjectPermissionMapperInterface<PermissionModelType> {

	public Class<ObjectPermissionModelInterface> getQueryClass();
	
}
