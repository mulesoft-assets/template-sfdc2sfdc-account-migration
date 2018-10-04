
# Template: Salesforce Org to Org Account Migration

Moves a large set of accounts from one Salesforce Org to another. You can trigger this manually or programmatically with an HTTP call. 

Accounts are upserted so that the migration can be run multiple times without worrying about creating duplicates. This template uses batch to efficiently process many records at a time.

![fb97a15f-f1d5-4a4d-81bd-60e2e8755576-image.png](https://exchange2-file-upload-service-kprod.s3.us-east-1.amazonaws.com:443/fb97a15f-f1d5-4a4d-81bd-60e2e8755576-image.png)

**Note**: Any references in the video to DataMapper have been updated in the template with DataWeave transformations.

[//]: # (![]\(https://www.youtube.com/embed/6ElLFqGUfNc?wmode=transparent\))
[![YouTube Video](http://img.youtube.com/vi/6ElLFqGUfNc/0.jpg)](https://www.youtube.com/watch?v=6ElLFqGUfNc)

# License Agreement

This template is subject to the conditions of the [MuleSoft License Agreement](https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf).

Review the terms of the license before downloading and using this template. You can use this template for free with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case

As a Salesforce administrator I want to migrate accounts from one Salesforce organization to another one.

This template serves as a foundation for the process of migrating accounts from one Salesforce instance to another, and specifying the filtering criteria and desired behavior when an account already exists in the destination org. 

As implemented, this template leverages the Mule batch module.

The batch job is divided into _Process_ and _On Complete_ stages.

The migration process starts by fetching all the existing accounts that match the filter criteria from Salesforce Org A.

On _Process_ stage matches accounts that are grouped and inserted or updated into Salesforce Org B.

Finally during the _On Complete_ stage, the template both outputs statistics data to the console and sends a notification email with the results of the batch execution.        

The template is covered by the integration tests using the MUnit. To be able to run the tests, see the example configuration of the test property file.

# Considerations

To make this template run, there are certain preconditions that must be considered. All of them deal with the preparations in both, that must be made for all to run smoothly.

Failing to do so can lead to unexpected behavior of the template.

## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: [Salesforce: Checking Field Accessibility for a Particular Field](https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US "Salesforce: Checking Field Accessibility for a Particular Field")
- Can I modify the Field Access Settings? How? See: [Salesforce: Modifying Field Access Settings](https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US "Salesforce: Modifying Field Access Settings")

### As a Data Source

If the user who configured the template for the source system does not have at least _read only_ permissions for the fields that are fetched, then an _InvalidFieldFault_ API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault 
[ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting 
to use a custom field, be sure to append the '__c' after the custom 
field name. Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.

# Run it!

Simple steps to get Salesforce to Salesforce Account Migration running.

## Running On Premises

In this section we help you run your template on your computer.

### Where to Download Anypoint Studio and the Mule Runtime

If you are a newcomer to Mule, here is where to get the tools.

- [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
- [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)

### Importing a Template into Studio

In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your

Anypoint Platform credentials, search for the template, and click **Open**.

### Running on Studio

After you import your template into Anypoint Studio, follow these steps to run it:

- Locate the properties file `mule.dev.properties`, in src/main/resources.
- Complete all the properties required as per the examples in the "Properties to Configure" section.
- Right click the template project folder.
- Hover your mouse over `Run as`
- Click `Mule Application (configure)`
- Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
- Click `Run`

### Running on Mule Standalone

Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 

## Running on CloudHub

While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.

After your app is running, if you chose `sfdcaccountmigration` as the domain name to trigger the use case, you can browse to `http://sfdcaccountmigration.cloudhub.io/migrateaccounts` to run the app, and the report gets sent to the emails you configured.

### Deploying your Anypoint Template on CloudHub

Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this

## Properties to Configure

To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.

### Application Configuration

**HTTP Connector Configuration**

- http.port `9090` 

**Batch Aggregator Configuration**

- page.size `1000`

**Salesforce Connector Configuration for Company A**

- sfdc.a.username `bob.dylan@orga`
- sfdc.a.password `DylanPassword123`
- sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`

**Salesforce Connector Configuration for Company B**

- sfdc.b.username `joan.baez@orgb`
- sfdc.b.password `JoanBaez456`
- sfdc.b.securityToken `ces56arl7apQs56XTddf34X`

**SMTP Services Configuration**

- smtp.host `smtp.example.com`
- smtp.port `587`
- smtp.user `exampleuser`
- smtp.password `examplepassword`

**Email Details**

- mail.from `your.email@example.com`
- mail.to `your.email@example.com`
- mail.subject `Mail subject`

# API Calls

Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount is important. This template calls the API using this formula:

_**1 + X + X / ${page.size}**_

_**X**_ is the number of accounts to be synchronized on each run. 

Divide by _**${page.size}**_ because by default, accounts are gathered in groups of ${page.size} for each Upsert API Call in the commit step.    

For instance if 10 records are fetched from origin instance, then 12 API calls are made (1 + 10 + 1).

# Customize It!

This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.

As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

- config.xml
- businessLogic.xml
- endpoints.xml
- errorHandling.xml

## config.xml

Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the _Global Element_ tab.

## businessLogic.xml

The functional aspect of the template is implemented in this XML file, directed by one flow responsible of excecuting the logic.

For the purpose of this particular template the _mainFlow_ uses a Mule batch job, which handles all the logic.

## endpoints.xml

This is the file where you specify the inbound and outbound sides of your integration app.

This template only uses an HTTP Listener to trigger the use case.

**HTTP Listener** - Start Report Generation

- `${http.port}` is set as a property to be defined either in a property file or in the CloudHub environment variables.
- The path configured by default is `migrateaccounts` and you are free to change it to one you prefer.
- The host name for all endpoints in your CloudHub configuration should be defined as `localhost`. CloudHub routes requests from your application domain URL to the endpoint.

+ The endpoint is configured as a _request-response_ since as a result of calling it the response is the total of accounts synced and filtered by the criteria specified.

## errorHandling.xml

This is the right place to handle how your integration reacts depending on the different exceptions.

This file provides error handling that is referenced by the main flow in the business logic.
