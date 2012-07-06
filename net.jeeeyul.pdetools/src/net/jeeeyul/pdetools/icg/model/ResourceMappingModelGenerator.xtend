package net.jeeeyul.pdetools.icg.model

import java.util.Stack
import net.jeeeyul.pdetools.icg.model.imageResource.FieldNameOwner
import net.jeeeyul.pdetools.icg.model.imageResource.ImageResourceFactory
import net.jeeeyul.pdetools.icg.model.imageResource.Palette
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IFolder
import org.eclipse.core.resources.IResource
import net.jeeeyul.pdetools.icg.model.imageResource.ImageFile

class ResourceMappingModelGenerator {
	Stack<GenerationContext> stack
	
	new(){
		stack = new Stack<GenerationContext>();
		pushContext(null);
	}
	
	def Palette generatePalette(IFolder folder){
		var palette = ImageResourceFactory::eINSTANCE.createPalette();
		palette.folder = folder
		palette.assigneFieldName(folder.name.safeFieldName);
		if(currentContext.palette != null){
			currentContext.palette.subPalettes += palette
		}
	
		pushContext(palette);
		{
			folder.sortedMember.filter(typeof(IFolder)).forEach[it.generatePalette];
			
			for(eachFile : folder.sortedMember.filter(typeof(IFile))){
				palette.imageFiles += eachFile.generateImageFile();	
			}
		}
		popContext();
		
		return palette
	}
	
	def ImageFile generateImageFile(IFile file) {
		return  ImageResourceFactory::eINSTANCE.createImageFile() => [
			it.file = file
			assigneFieldName(file.fullPath.removeFileExtension.lastSegment.safeFieldName)
		]
	}

	
	def private popContext() { 
		stack.pop();
	}
	
	def private assigneFieldName(FieldNameOwner fieldNameOwner, String preferName){
		if(!currentContext.isRegisterdFieldName(preferName)){
			fieldNameOwner.fieldName = preferName;
			currentContext.registerFieldName(preferName)
		}else{
			var step = 2
			var newName = preferName + "_" + step ;
			while(currentContext.isRegisterdFieldName(newName)){
				step = step + 1
				newName = preferName + "_" + step;
			}
			fieldNameOwner.fieldName = newName;
			currentContext.registerFieldName(newName);
		}
	}
	
	def private currentContext(){
		stack.peek
	}
	
	def private pushContext(Palette palette){
		stack.push(new net.jeeeyul.pdetools.icg.model.GenerationContext(palette));
	}
	
	def private safeFieldName(String preferName){
		var result = preferName.replaceAll("[^a-zA-Z0-9_]", "_").toUpperCase;
		if(result.matches("[0-9].*")){
			result = "_" + result
		}
		return result;
	}
	
	def private IResource[] sortedMember(IFolder folder){
		folder.members.sort[a, b|
			if(a instanceof IFolder && b instanceof IFile){
				return -1;
			}
			else if(a instanceof IFile && b instanceof IFolder){
				return 1;
			}
			else{
				return a.name.compareTo(b.name)	
			}
		];
	}
}