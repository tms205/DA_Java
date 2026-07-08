IF OBJECT_ID(N'dbo.CustomerFeedback', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CustomerFeedback (
        feedback_id INT IDENTITY PRIMARY KEY,
        customer_id INT NOT NULL,
        reason NVARCHAR(500) NOT NULL,
        expectation NVARCHAR(1000) NOT NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_CustomerFeedback_Customer
            FOREIGN KEY (customer_id) REFERENCES dbo.Customer(customer_id)
    );
END;
