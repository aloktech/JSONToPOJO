# JSONToPOJO
**Generate POJO from JSON file**
<br/><br/>
Generating Java files from a json file. The generated java file have implement Serializable Interface and used Lombok, Bean Validation and Fast Json as library.
<br/><br/>
**java -jar JSONToPOJO-1.0-SNAPSHOT-jar-with-dependencies.jar &lt;Json file&gt; &lt;Output folder&gt;**<br/><br/>
**java -jar JSONToPOJO-1.0-SNAPSHOT-jar-with-dependencies.jar inputSchema.json test**<br/><br/><br/>
Input json template file:
```
{
    "package_name": "Dummy Data",
    "class_name": "Dummy Data",
    "validation":true,
    "data": {
        "Dummy Data"
    }
}
```
Sample Input json file
```
{
    "package_name": "sample",
    "class_name": "SampleClass",
    "validation":true,
    "data": {
        "sample_one": {
            "sample_one_one": 219186,
            "sample_one_two": "Dummy Data",
            "sample_one_three": "Dummy Data"
        },
        "sample_two": "Dummy Data",
        "sample_three": [{
            "sample_three_one": true,
            "sample_three_two": "Dummy Data",
            "sample_three_three": "Dummy Data"
        }]
    }
}
```

Generated Java Code
```
@Getter
@Setter
public class SampleClass implements Serializable {

    @Valid
    @NotNull(message = "{sample.one}", groups = FirstValidation.class)
    @JSONField(name = "sample_one")
    private SampleOne sampleOne;

    @NotNull(message = "{sample.two}", groups = FirstValidation.class)
    @NotEmpty(message = "{sample.two}", groups = SecondValidation.class)
    @JSONField(name = "sample_two")
    private String sampleTwo;

    @JSONField(name = "sample_three")
    private List<SampleThree> sampleThree;
}
```

SampleOne, SampleThree are generated Java Class
