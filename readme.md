# SupplierDataWithPagination
Ariba Open API - Supplier Data With Pagination

Java Client 4 for Supplier Data With Pagination Ariba API

The client authenticate through OAuth to Ariba API Supplier Data With Pagination endpoint and save the result to a csv file at argument specified path and insert records to a MySQL database.

Arguments:
- ph: proxy host (optional - default direct)
- pp: proxy port number (optional - default direct)
- cp: csv path (optional - default user download path)
- cn: csv file name (optional - default suppliers.csv)
- v : specific vendor ariba code (optional - all if not specified)
- dh: mysql host name (optional - default localhost)
- dp: mysql port number (optional - default 3306)
- du: mysql user (optional - default root)
- dk: mysql password (optional - default techedge)
- dt: mysql table name (optional - default default.suppliers)
- k : ariba open api secret key base64
- l : log level (optional - default 0, max 2)

MySQL Table Fields:
- timestamp: datetime(1) - PK
- sm_vendor_id: varchar(8) - PK
- counter: int(11) - PK
- erp_vendor_id: varchar(10)
- an_id: varchar(30)
- supplier_name: varchar(160)
- registration_status: varchar(50)
- integrated_to_erp: varchar(50)
- address_line_1: varchar(50)
- address_line_2: varchar(50)
- address_line_3: varchar(50)
- address_city: varchar(45)
- address_country_code:  varchar(3)
- address_region_code:  varchar(3)
- address_po_box:  varchar(50)
- address_postal_code:  varchar(50)
- qualification_status:  varchar(50)
- preferred_status:  varchar(1)
- category:  varchar(20)
- region:  varchar(4)
- business_unit:  varchar(4)
