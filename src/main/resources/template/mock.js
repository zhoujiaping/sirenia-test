(function(){
	return {
		'TestController#one':function(json,dataSetId){
			print(json)
			print(dataSetId)
			
			return JSON.stringify({
				mock:'MOCK'
			})
		}
	};
})();