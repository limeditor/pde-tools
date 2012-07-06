package net.jeeeyul.pdetools.icg

import net.jeeeyul.pdetools.icg.model.imageResource.Palette
import net.jeeeyul.pdetools.icg.model.imageResource.ImageFile

class ImageCosntantGenerator {
	@Property ICGConfiguration config;
	@Property Palette rootPalette

	def generate()'''
		package «config.generatePackageName»;
		
		public class «config.generateClassName»{
			«FOR eachPalette :  rootPalette.subPalettes»
				«eachPalette.generateSubPalette»
			«ENDFOR»
			«FOR eachFile : rootPalette.imageFiles»
				«eachFile.generateField()»
			«ENDFOR»
		}
	'''
	def private generateSubPalette(Palette palette) '''
		public static interface «palette.fieldName»{
			«FOR eachFile : palette.imageFiles»
				«eachFile.generateField()»
			«ENDFOR»
		}
	'''
	def private generateField(ImageFile file)'''
		public static final String «file.fieldName» = "«file.file.projectRelativePath.toPortableString»";
	'''
}